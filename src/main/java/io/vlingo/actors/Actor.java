// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.actors;

import io.vlingo.actors.testkit.TestEnvironment;
import io.vlingo.actors.testkit.TestState;
import io.vlingo.actors.testkit.TestStateView;

public abstract class Actor implements Startable, Stoppable, TestStateView {
  final LifeCycle lifeCycle;

  public Address address() {
    return lifeCycle.address();
  }

  public DeadLetters deadLetters() {
    return lifeCycle.environment.stage.world().deadLetters();
  }

  @Override
  public void start() {
  }

  @Override
  public boolean isStopped() {
    return lifeCycle.isStopped();
  }

  @Override
  public void stop() {
    if (!isStopped()) {
      if (lifeCycle.address().id() != World.DEADLETTERS_ID) {
        lifeCycle.stop(this);
      }
    }
  }

  public TestState viewTestState() {
    // override for concrete actor state
    return new TestState();
  }

  @Override
  public boolean equals(final Object other) {
    if (other == null || other.getClass() != this.getClass()) {
      return false;
    }
    
    return address().equals(((Actor) other).lifeCycle.address());
  }

  @Override
  public int hashCode() {
    return lifeCycle.hashCode();
  }

  @Override
  public String toString() {
    return "Actor[type=" + this.getClass().getSimpleName() + " address=" + address() + "]";
  }

  protected Actor() {
    final Environment maybeEnvironment = ActorFactory.threadLocalEnvironment.get();
    this.lifeCycle = new LifeCycle(maybeEnvironment != null ? maybeEnvironment : new TestEnvironment());
    ActorFactory.threadLocalEnvironment.set(null);
    this.lifeCycle.sendStart(this);
  }

  protected <T> T childActorFor(final Definition definition, final Class<T> protocol) {
    if (definition.supervisor() != null) {
      return lifeCycle.environment.stage.actorFor(definition, protocol, this, definition.supervisor(), logger());
    } else {
      if (this instanceof Supervisor) {
        return lifeCycle.environment.stage.actorFor(definition, protocol, this, lifeCycle.lookUpProxy(Supervisor.class), logger());
      } else {
        return lifeCycle.environment.stage.actorFor(definition, protocol, this, null, logger());
      }
    }
  }

  protected Definition definition() {
    return lifeCycle.definition();
  }

  protected Logger logger() {
    return lifeCycle.environment.logger;
  }

  protected Actor parent() {
    if (lifeCycle.environment.isSecured()) {
      throw new IllegalStateException("A secured actor cannot provide its parent.");
    }
    return lifeCycle.environment.parent;
  }

  protected void secure() {
    lifeCycle.secure();
  }

  protected <T> T selfAs(final Class<T> protocol) {
    return lifeCycle.environment.stage.actorProxyFor(protocol, this, lifeCycle.environment.mailbox);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected OutcomeInterest selfAsOutcomeInterest(final Object reference) {
    final OutcomeAware outcomeAware = lifeCycle.environment.stage.actorProxyFor(OutcomeAware.class, this, lifeCycle.environment.mailbox);
    return new OutcomeInterestActorProxy(outcomeAware, reference);
  }

  protected final Stage stage() {
    if (lifeCycle.environment.isSecured()) {
      throw new IllegalStateException("A secured actor cannot provide its stage.");
    }
    return lifeCycle.environment.stage;
  }

  protected Stage stageNamed(final String name) {
    return lifeCycle.environment.stage.world().stageNamed(name);
  }

  //=======================================
  // stowing/dispersing
  //=======================================

  protected boolean isDispersing() {
    return lifeCycle.environment.stowage.isDispersing();
  }

  protected void disperseStowedMessages() {
    lifeCycle.environment.stowage.dispersingMode();
  }

  protected boolean isStowing() {
    return lifeCycle.environment.stowage.isStowing();
  }

  protected void stowMessages() {
    lifeCycle.environment.stowage.stowingMode();
  }

  //=======================================
  // life cycle overrides
  //=======================================

  protected void beforeStart() {
    // override
  }

  protected void afterStop() {
    // override
  }

  protected void beforeRestart(final Throwable reason) {
    // override
    lifeCycle.afterStop(this);
  }

  protected void afterRestart(final Throwable reason) {
    // override
    lifeCycle.beforeStart(this);
  }

  protected void beforeResume(final Throwable reason) {
    // override
  }
}

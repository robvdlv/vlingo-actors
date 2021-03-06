// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.actors.plugin.mailbox.agronampscarrayqueue;

import static org.junit.Assert.assertEquals;

import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

import io.vlingo.actors.Actor;
import io.vlingo.actors.ActorsTest;
import io.vlingo.actors.LocalMessage;
import io.vlingo.actors.Mailbox;

public class ManyToOneConcurrentArrayQueueDispatcherTest extends ActorsTest {
  private static final int MailboxSize = 64;
  
  @Test
  public void testClose() throws Exception {
    final ManyToOneConcurrentArrayQueueDispatcher dispatcher =
            new ManyToOneConcurrentArrayQueueDispatcher(MailboxSize, 2, 4, 10);
    
    dispatcher.start();
    
    final Mailbox mailbox = dispatcher.mailbox();
    
    until(MailboxSize);
    
    final CountTakerActor actor = new CountTakerActor();
    
    for (int count = 1; count <= MailboxSize; ++count) {
      final int countParam = count;
      final Consumer<CountTaker> consumer = (consumerActor) -> consumerActor.take(countParam);
      final LocalMessage<CountTaker> message = new LocalMessage<CountTaker>(actor, CountTaker.class, consumer, "take(int)");
      
      mailbox.send(message);
    }
    
    until.completes();

    dispatcher.close();
    
    final int neverRevieved = MailboxSize * 2;
    
    for (int count = MailboxSize + 1; count <= neverRevieved; ++count) {
      final int countParam = count;
      final Consumer<CountTaker> consumer = (consumerActor) -> consumerActor.take(countParam);
      final LocalMessage<CountTaker> message = new LocalMessage<CountTaker>(actor, CountTaker.class, consumer, "take(int)");
      
      mailbox.send(message);
    }

    until(0).completes();

    assertEquals(MailboxSize, CountTakerActor.highest);
  }

  @Test
  public void testBasicDispatch() throws Exception {
    final int mailboxSize = 64;
    
    until(MailboxSize);
    
    final ManyToOneConcurrentArrayQueueDispatcher dispatcher =
            new ManyToOneConcurrentArrayQueueDispatcher(mailboxSize, 2, 4, 10);
    
    dispatcher.start();
    
    final Mailbox mailbox = dispatcher.mailbox();
    
    final CountTakerActor actor = new CountTakerActor();
    
    for (int count = 1; count <= mailboxSize; ++count) {
      final int countParam = count;
      final Consumer<CountTaker> consumer = (consumerActor) -> consumerActor.take(countParam);
      final LocalMessage<CountTaker> message = new LocalMessage<CountTaker>(actor, CountTaker.class, consumer, "take(int)");
      
      mailbox.send(message);
    }
    
    until.completes();
    
    assertEquals(mailboxSize, CountTakerActor.highest);
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
    
    CountTakerActor.highest = 0;
  }
  
  public static interface CountTaker {
    void take(final int count);
  }
  
  public static class CountTakerActor extends Actor implements CountTaker {
    public static int highest;
    
    @Override
    public void take(final int count) {
      if (count > highest) {
        highest = count;
      }
      until.happened();
    }
  }
}

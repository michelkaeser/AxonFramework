/*
 * Copyright (c) 2010-2012. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.commandhandling;

import org.axonframework.common.Registration;
import org.axonframework.domain.StubAggregate;
import org.axonframework.eventhandling.AbstractEventBus;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.EventProcessor;
import org.axonframework.eventsourcing.DomainEventMessage;
import org.axonframework.eventsourcing.DomainEventStream;
import org.axonframework.eventsourcing.EventSourcingRepository;
import org.axonframework.eventsourcing.SimpleDomainEventStream;
import org.axonframework.eventstore.EventStore;
import org.axonframework.messaging.unitofwork.CurrentUnitOfWork;
import org.axonframework.messaging.unitofwork.DefaultUnitOfWork;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

/**
 *
 */
public class CommandHandlingTest {

    private EventSourcingRepository<StubAggregate> repository;
    private String aggregateIdentifier;
    private StubEventStore stubEventStore;

    @Before
    public void setUp() {
        stubEventStore = new StubEventStore();
        repository = new EventSourcingRepository<>(StubAggregate.class, stubEventStore);
        repository.setEventBus(stubEventStore);
        aggregateIdentifier = "testAggregateIdentifier";
    }

    @Test
    public void testCommandHandlerLoadsSameAggregateTwice() {
        startAndGetUnitOfWork();
        StubAggregate stubAggregate = new StubAggregate(aggregateIdentifier);
        stubAggregate.doSomething();
        repository.add(stubAggregate);
        CurrentUnitOfWork.commit();

        startAndGetUnitOfWork();
        repository.load(aggregateIdentifier).doSomething();
        repository.load(aggregateIdentifier).doSomething();
        CurrentUnitOfWork.commit();

        DomainEventStream es = stubEventStore.readEvents(aggregateIdentifier);
        assertTrue(es.hasNext());
        assertEquals((Object) 0L, es.next().getSequenceNumber());
        assertTrue(es.hasNext());
        assertEquals((Object) 1L, es.next().getSequenceNumber());
        assertTrue(es.hasNext());
        assertEquals((Object) 2L, es.next().getSequenceNumber());
        assertFalse(es.hasNext());
    }

    private UnitOfWork startAndGetUnitOfWork() {
        UnitOfWork uow = DefaultUnitOfWork.startAndGet(null);
        uow.resources().put(EventBus.KEY, stubEventStore);
        return uow;
    }

    private static class StubEventStore extends AbstractEventBus implements EventStore {

        private List<DomainEventMessage> storedEvents = new LinkedList<>();


        @Override
        public void appendEvents(List<DomainEventMessage<?>> events) {
            storedEvents.addAll(events);
        }

        @Override
        public DomainEventStream readEvents(String identifier) {
            return new SimpleDomainEventStream(new ArrayList<>(storedEvents));
        }

        @Override
        public DomainEventStream readEvents(String identifier, long firstSequenceNumber,
                                            long lastSequenceNumber) {
            return new SimpleDomainEventStream(
                    storedEvents.stream()
                                .filter(m -> m.getSequenceNumber() >= firstSequenceNumber
                                        && m.getSequenceNumber() <= lastSequenceNumber)
                                .collect(toList()));
        }

        @Override
        protected void commit(List<EventMessage<?>> events) {
            appendEvents(events.stream().map(event -> (DomainEventMessage<?>) event).collect(Collectors.toList()));
        }

        @Override
        public Registration subscribe(EventProcessor eventProcessor) {
            throw new UnsupportedOperationException();
        }
    }
}

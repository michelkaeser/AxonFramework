package org.axonframework.messaging.annotation;

import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.junit.jupiter.api.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class validating the {@link AnnotatedMessageHandlingMember}.
 *
 * @author Steven van Beelen
 */
class AnnotatedMessageHandlingMemberTest {

    private AnnotatedMessageHandlingMember<AnnotatedHandler> testSubject;

    @BeforeEach
    void setUp() {
        testSubject = new AnnotatedMessageHandlingMember<>(
                AnnotatedHandler.class.getMethods()[0],
                EventMessage.class,
                String.class,
                ClasspathParameterResolverFactory.forClass(AnnotatedHandler.class)
        );
    }

    @Test
    void testCanHandleMessageType() {
        assertTrue(testSubject.canHandleMessageType(EventMessage.class));
        assertFalse(testSubject.canHandleMessageType(CommandMessage.class));
    }

    @Test
    void testAttributeReturnsNonEmptyOptionalForMatchingAttributeKey() {
        Optional<Object> resultMessageType = testSubject.attribute(HandlerAttributes.MESSAGE_TYPE);
        Optional<Object> resultPayloadType = testSubject.attribute(HandlerAttributes.PAYLOAD_TYPE);

        assertTrue(resultMessageType.isPresent());
        assertEquals(EventMessage.class, resultMessageType.get());

        assertTrue(resultPayloadType.isPresent());
        assertEquals(Object.class, resultPayloadType.get());
    }

    @SuppressWarnings("unused")
    private static class AnnotatedHandler {

        @EventHandler
        public void handlingMethod(String event) {

        }
    }
}
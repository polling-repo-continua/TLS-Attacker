/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2016 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.tls.protocol.parser;

import de.rub.nds.tlsattacker.tls.constants.HeartbeatByteLength;
import de.rub.nds.tlsattacker.tls.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.tls.protocol.message.HeartbeatMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Robert Merget - robert.merget@rub.de
 */
public class HeartbeatMessageParser extends ProtocolMessageParser<HeartbeatMessage> {

    private static final Logger LOGGER = LogManager.getLogger("PARSER");
    
    public HeartbeatMessageParser(int startposition, byte[] array, ProtocolVersion version) {
        super(startposition, array, version);
    }

    @Override
    protected HeartbeatMessage parseMessageContent() {
        HeartbeatMessage message = new HeartbeatMessage();
        message.setHeartbeatMessageType(parseByteField(HeartbeatByteLength.TYPE));
        message.setPayloadLength(parseIntField(HeartbeatByteLength.PAYLOAD_LENGTH));
        message.setPayload(parseByteArrayField(message.getPayloadLength().getValue()));
        message.setPadding(parseByteArrayField(getBytesLeft()));
        return message;
    }

}

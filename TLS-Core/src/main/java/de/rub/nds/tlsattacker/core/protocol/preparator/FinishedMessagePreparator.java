/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.core.protocol.preparator;

import de.rub.nds.tlsattacker.core.constants.HandshakeByteLength;
import de.rub.nds.tlsattacker.core.constants.PRFAlgorithm;
import de.rub.nds.tlsattacker.core.crypto.PseudoRandomFunction;
import de.rub.nds.tlsattacker.core.protocol.message.FinishedMessage;
import de.rub.nds.tlsattacker.transport.ConnectionEnd;
import de.rub.nds.modifiablevariable.util.ArrayConverter;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.workflow.chooser.Chooser;

/**
 *
 * @author Robert Merget - robert.merget@rub.de
 */
public class FinishedMessagePreparator extends HandshakeMessagePreparator<FinishedMessage> {

    private byte[] verifyData;
    private final FinishedMessage msg;

    public FinishedMessagePreparator(Chooser chooser, FinishedMessage message) {
        super(chooser, message);
        this.msg = message;
    }

    @Override
    public void prepareHandshakeMessageContents() {
        verifyData = computeVerifyData();

        prepareVerifyData(msg);
    }

    private byte[] computeVerifyData() {
        PRFAlgorithm prfAlgorithm = chooser.getPRFAlgorithm();
        byte[] masterSecret = chooser.getMasterSecret();
        CipherSuite suite = chooser.getSelectedCipherSuite();
        ProtocolVersion version = chooser.getSelectedProtocolVersion();
        byte[] handshakeMessageHash = chooser.getDigest().digest(version, suite);

        if (chooser.getConfig().getConnectionEnd() == ConnectionEnd.SERVER) {
            // TODO put this in seperate config option
            return PseudoRandomFunction.compute(prfAlgorithm, masterSecret, PseudoRandomFunction.SERVER_FINISHED_LABEL,
                    handshakeMessageHash, HandshakeByteLength.VERIFY_DATA);
        } else {
            return PseudoRandomFunction.compute(prfAlgorithm, masterSecret, PseudoRandomFunction.CLIENT_FINISHED_LABEL,
                    handshakeMessageHash, HandshakeByteLength.VERIFY_DATA);
        }
    }

    private void prepareVerifyData(FinishedMessage msg) {
        msg.setVerifyData(verifyData);
        LOGGER.debug("VerifyData: " + ArrayConverter.bytesToHexString(msg.getVerifyData().getValue()));
    }

}

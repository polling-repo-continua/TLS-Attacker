/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.core.protocol.preparator;

import de.rub.nds.tlsattacker.core.constants.HashAlgorithm;
import de.rub.nds.tlsattacker.core.constants.SignatureAlgorithm;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;
import de.rub.nds.tlsattacker.core.exceptions.PreparationException;
import de.rub.nds.tlsattacker.core.protocol.message.DHEServerKeyExchangeMessage;
import de.rub.nds.tlsattacker.core.workflow.TlsContext;
import de.rub.nds.modifiablevariable.util.ArrayConverter;
import java.security.Security;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Robert Merget - robert.merget@rub.de
 */
public class DHEServerKeyExchangePreparatorTest {

    private TlsContext context;
    private DHEServerKeyExchangePreparator preparator;
    private DHEServerKeyExchangeMessage message;

    public DHEServerKeyExchangePreparatorTest() {
    }

    @Before
    public void setUp() {
        context = new TlsContext();
        message = new DHEServerKeyExchangeMessage();
        preparator = new DHEServerKeyExchangePreparator(context, message);
    }

    /**
     * Test of prepareHandshakeMessageContents method, of class
     * DHEServerKeyExchangePreparator.
     */
    @Test
    public void testPrepare() {
        context.getConfig()
                .setFixedDHg(
                        ArrayConverter
                                .hexStringToByteArray("a51883e9ac0539859df3d25c716437008bb4bd8ec4786eb4bc643299daef5e3e5af5863a6ac40a597b83a27583f6a658d408825105b16d31b6ed088fc623f648fd6d95e9cefcb0745763cddf564c87bcf4ba7928e74fd6a3080481f588d535e4c026b58a21e1e5ec412ff241b436043e29173f1dc6cb943c09742de989547288"));
        context.getConfig()
                .setFixedDHModulus(
                        ArrayConverter
                                .hexStringToByteArray("da3a8085d372437805de95b88b675122f575df976610c6a844de99f1df82a06848bf7a42f18895c97402e81118e01a00d0855d51922f434c022350861d58ddf60d65bc6941fc6064b147071a4c30426d82fc90d888f94990267c64beef8c304a4b2b26fb93724d6a9472fa16bc50c5b9b8b59afb62cfe9ea3ba042c73a6ade35"));
        context.setClientRandom(ArrayConverter.hexStringToByteArray("AABBCCDD"));
        context.setServerRandom(ArrayConverter.hexStringToByteArray("AABBCCDD"));
        // Set Signature and Hash Algorithm
        List<SignatureAndHashAlgorithm> SigAndHashList = new LinkedList<>();
        SigAndHashList.add(new SignatureAndHashAlgorithm(SignatureAlgorithm.RSA, HashAlgorithm.SHA1));
        SigAndHashList.add(new SignatureAndHashAlgorithm(SignatureAlgorithm.DSA, HashAlgorithm.MD5));
        context.getConfig().setSupportedSignatureAndHashAlgorithms(SigAndHashList);
        // Generate RSA key pair
        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException ex) {
            throw new PreparationException("Could not generate a new Key", ex);
        }
        context.getConfig().setPrivateKey(keyGen.genKeyPair().getPrivate());
        // Test
        preparator.prepareHandshakeMessageContents();
        System.out.println("" + ArrayConverter.bytesToHexString(message.getG().getValue(), false));
        System.out.println("" + ArrayConverter.bytesToHexString(message.getP().getValue(), false));

        assertArrayEquals(
                ArrayConverter
                        .hexStringToByteArray("a51883e9ac0539859df3d25c716437008bb4bd8ec4786eb4bc643299daef5e3e5af5863a6ac40a597b83a27583f6a658d408825105b16d31b6ed088fc623f648fd6d95e9cefcb0745763cddf564c87bcf4ba7928e74fd6a3080481f588d535e4c026b58a21e1e5ec412ff241b436043e29173f1dc6cb943c09742de989547288"),
                message.getG().getValue());
        assertArrayEquals(
                ArrayConverter
                        .hexStringToByteArray("da3a8085d372437805de95b88b675122f575df976610c6a844de99f1df82a06848bf7a42f18895c97402e81118e01a00d0855d51922f434c022350861d58ddf60d65bc6941fc6064b147071a4c30426d82fc90d888f94990267c64beef8c304a4b2b26fb93724d6a9472fa16bc50c5b9b8b59afb62cfe9ea3ba042c73a6ade35"),
                message.getP().getValue());
        assertArrayEquals(ArrayConverter.hexStringToByteArray("AABBCCDD"), message.getComputations().getClientRandom()
                .getValue());
        assertArrayEquals(ArrayConverter.hexStringToByteArray("AABBCCDD"), message.getComputations().getServerRandom()
                .getValue());
        assertTrue(SignatureAlgorithm.RSA.getValue() == message.getSignatureAlgorithm().getValue());
        assertTrue(HashAlgorithm.SHA1.getValue() == message.getHashAlgorithm().getValue());
        assertNotNull(message.getSignature().getValue());
        assertNotNull(message.getSignatureLength().getValue());
    }
}
/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.attacks.impl;

import de.rub.nds.modifiablevariable.VariableModification;
import de.rub.nds.modifiablevariable.bytearray.ByteArrayModificationFactory;
import de.rub.nds.modifiablevariable.bytearray.ModifiableByteArray;
import de.rub.nds.modifiablevariable.util.ArrayConverter;
import de.rub.nds.tlsattacker.attacks.config.PaddingOracleCommandConfig;
import de.rub.nds.tlsattacker.attacks.util.response.EqualityError;
import de.rub.nds.tlsattacker.attacks.util.response.FingerPrintChecker;
import de.rub.nds.tlsattacker.attacks.util.response.ResponseExtractor;
import de.rub.nds.tlsattacker.attacks.util.response.ResponseFingerprint;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.AlgorithmResolver;
import de.rub.nds.tlsattacker.core.constants.RunningModeType;
import de.rub.nds.tlsattacker.core.protocol.message.AlertMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ApplicationMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ProtocolMessage;
import de.rub.nds.tlsattacker.core.record.AbstractRecord;
import de.rub.nds.tlsattacker.core.record.Record;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.util.LogLevel;
import de.rub.nds.tlsattacker.core.workflow.WorkflowExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowExecutorFactory;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowConfigurationFactory;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Executes a padding oracle attack check. It logs an error in case the tested
 * server is vulnerable to poodle.
 */
public class PaddingOracleAttacker extends Attacker<PaddingOracleCommandConfig> {

    private static final Logger LOGGER = LogManager.getLogger(PaddingOracleAttacker.class);

    private final Config tlsConfig;

    public PaddingOracleAttacker(PaddingOracleCommandConfig paddingOracleConfig) {
        super(paddingOracleConfig, false);
        tlsConfig = paddingOracleConfig.createConfig();
    }

    @Override
    public void executeAttack() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public State executeTlsFlow(Record record) {
        tlsConfig.setAddSignatureAndHashAlgrorithmsExtension(true);
        WorkflowTrace trace = new WorkflowConfigurationFactory(tlsConfig).createWorkflowTrace(
                WorkflowTraceType.HANDSHAKE, RunningModeType.CLIENT);
        ApplicationMessage applicationMessage = new ApplicationMessage(tlsConfig);
        SendAction sendAction = new SendAction(applicationMessage);
        sendAction.setRecords(new LinkedList<AbstractRecord>());
        sendAction.getRecords().add(record);
        trace.addTlsAction(sendAction);
        AlertMessage alertMessage = new AlertMessage(tlsConfig);
        trace.addTlsAction(new ReceiveAction(alertMessage));

        State state = new State(tlsConfig, trace);

        WorkflowExecutor workflowExecutor = WorkflowExecutorFactory.createWorkflowExecutor(
                tlsConfig.getWorkflowExecutorType(), state);

        workflowExecutor.executeWorkflow();
        return state;
    }

    private List<Record> createRecordsWithPlainData(int blocksize, int macSize) {
        List<Record> records = new LinkedList<>();
        for (int i = 0; i < 64; i++) {
            byte[] padding = createPaddingBytes(i);
            int messageSize = blocksize - (padding.length % blocksize);
            byte[] message = new byte[messageSize];
            byte[] plain = ArrayConverter.concatenate(message, padding);
            if (plain.length > macSize) {
                Record r = createRecordWithPlainData(plain);
                records.add(r);
            }
        }
        byte[] plain = new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
                (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
                (byte) 255 };
        if (plain.length > macSize) {
            Record r = createRecordWithPlainData(plain);
            records.add(r);
        }
        plain = new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
                (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
                (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
                (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
                (byte) 255 };
        if (plain.length > macSize) {
            Record r = createRecordWithPlainData(plain);
            records.add(r);
        }
        return records;
    }

    private Record createRecordWithPlainData(byte[] plain) {
        Record r = new Record(tlsConfig);
        ModifiableByteArray plainData = new ModifiableByteArray();
        VariableModification<byte[]> modifier = ByteArrayModificationFactory.explicitValue(plain);
        plainData.setModification(modifier);
        r.setPlainRecordBytes(plainData);
        return r;
    }

    private List<Record> createRecordsWithModifiedPadding() {
        List<Record> records = new LinkedList<>();

        Record r = new Record();
        ModifiableByteArray padding = new ModifiableByteArray();
        VariableModification<byte[]> modifier = ByteArrayModificationFactory.xor(new byte[] { 1 }, 0);
        padding.setModification(modifier);
        r.setPadding(padding);
        records.add(r);

        return records;
    }

    private List<Record> createRecordsWithModifiedMac() {
        List<Record> records = new LinkedList<>();

        Record r = new Record();
        ModifiableByteArray mac = new ModifiableByteArray();
        VariableModification<byte[]> modifier = ByteArrayModificationFactory.xor(new byte[] { 1, 1, 1 }, 0);
        mac.setModification(modifier);
        r.setMac(mac);
        records.add(r);

        return records;
    }

    private byte[] createPaddingBytes(int padding) {
        byte[] paddingBytes = new byte[padding + 1];
        for (int i = 0; i < paddingBytes.length; i++) {
            paddingBytes[i] = (byte) padding;
        }
        return paddingBytes;
    }

    @Override
    public Boolean isVulnerable() {
        int macSize = AlgorithmResolver.getMacAlgorithm(tlsConfig.getDefaultSelectedProtocolVersion(),
                tlsConfig.getDefaultSelectedCipherSuite()).getSize();
        int blockSize = AlgorithmResolver.getCipher(tlsConfig.getDefaultSelectedCipherSuite())
                .getNonceBytesFromHandshake();
        List<Record> records = new LinkedList<>();
        records.addAll(createRecordsWithPlainData(blockSize, macSize));
        records.addAll(createRecordsWithModifiedMac());
        records.addAll(createRecordsWithModifiedPadding());

        HashMap<Integer, List<ResponseFingerprint>> responseMap = new HashMap<>();
        for (Record record : records) {
            State state = executeTlsFlow(record);
            ResponseFingerprint fingerprint = ResponseExtractor.getFingerprint(state);
            AbstractRecord lastRecord = state.getWorkflowTrace().getLastSendingAction().getSendRecords()
                    .get(state.getWorkflowTrace().getLastSendingAction().getSendRecords().size() - 1);
            int length = ((Record) lastRecord).getLength().getValue();
            List<ResponseFingerprint> responseFingerprintList = responseMap.get(length);
            if (responseFingerprintList == null) {
                responseFingerprintList = new LinkedList<>();
                responseMap.put(length, responseFingerprintList);
            }
            responseFingerprintList.add(fingerprint);

        }
        for (List<ResponseFingerprint> list : responseMap.values()) {
            ResponseFingerprint fingerprint = list.get(0);
            for (int i = 1; i < list.size(); i++) {
                EqualityError error = FingerPrintChecker.checkEquality(fingerprint, list.get(i), true);
                if (error != EqualityError.NONE) {
                    LOGGER.log(LogLevel.CONSOLE_OUTPUT, "Found an equality Error: " + error);
                    LOGGER.info("Fingerprint1: " + fingerprint.toString());
                    LOGGER.info("Fingerprint2: " + list.get(i).toString());
                    return true;
                }
            }
        }

        return false;
    }

}

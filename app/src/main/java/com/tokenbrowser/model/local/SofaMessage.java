package com.tokenbrowser.model.local;

import com.tokenbrowser.model.sofa.SofaType;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class SofaMessage extends RealmObject {

    @PrimaryKey
    private String privateKey;
    private long creationTime;
    private @SofaType.Type int type;
    private @SendState.State int sendState;
    private String payload;
    private String attachmentFilePath;
    private User sender;

    public SofaMessage() {
        this.creationTime = System.currentTimeMillis();
        this.privateKey = UUID.randomUUID().toString();
    }

    // Setters

    private SofaMessage setType(final @SofaType.Type int type) {
        this.type = type;
        return this;
    }

    public SofaMessage setSendState(final @SendState.State int sendState) {
        this.sendState = sendState;
        return this;
    }

    public SofaMessage setSender(final User sender) {
        this.sender = sender;
        return this;
    }

    public SofaMessage setPayload(final String payload) {
        this.payload = payload;
        return this;
    }

    public SofaMessage setAttachmentFilePath(String attachmentFilePath) {
        this.attachmentFilePath = attachmentFilePath;
        return this;
    }

    // Getters

    public String getPrivateKey() {
        return this.privateKey;
    }

    public String getPayload() {
        return cleanPayload(this.payload);
    }

    public String getPayloadWithHeaders() {
        return this.payload;
    }

    public String getAttachmentFilePath() {
        return attachmentFilePath;
    }

    public User getSender() {
        return this.sender;
    }

    // Return message in the correct format for SOFA
    public String getAsSofaMessage() {
        // Strip away local-only data before sending via Signal
        final String matcher = "\"" + SofaType.LOCAL_ONLY_PAYLOAD + "\":\\{.*?\\},";
        return this.payload.replaceFirst(matcher, "");
    }

    public @SofaType.Type int getType() {
        return this.type;
    }

    public @SendState.State int getSendState() {
        return this.sendState;
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    // Helper functions

    public boolean isSentBy(final User sender) {
        return this.sender!= null && this.sender.equals(sender);
    }

    private String cleanPayload(final String payload) {
        final String regexString = "\\{.*\\}";
        final Pattern pattern = Pattern.compile(regexString);
        final Matcher m = pattern.matcher(payload);
        if (m.find()) {
            return m.group();
        }
        return payload;
    }

    private String getSofaHeader(final String payload) {
        final String regexString = "SOFA::.+?:";
        final Pattern pattern = Pattern.compile(regexString);
        final Matcher m = pattern.matcher(payload);
        if (m.find()) {
            return m.group();
        }
        return null;
    }

    public SofaMessage makeNew(
            final User sender,
            final String sofaPayload) {
        final String sofaHeader = getSofaHeader(sofaPayload);
        final @SofaType.Type int sofaType = SofaType.getType(sofaHeader);

        return  setSendState(SendState.STATE_SENDING)
                .setType(sofaType)
                .setSender(sender)
                .setPayload(sofaPayload);
    }

    public SofaMessage makeNew(final String sofaPayload) {
        final String sofaHeader = getSofaHeader(sofaPayload);
        final @SofaType.Type int sofaType = SofaType.getType(sofaHeader);

        return setType(sofaType)
                .setPayload(sofaPayload);
    }

    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof SofaMessage))return false;
        final SofaMessage otherSofaMessage = (SofaMessage) other;
        return otherSofaMessage.getPrivateKey().equals(this.privateKey);
    }

    @Override
    public int hashCode() {
        return privateKey.hashCode();
    }
}

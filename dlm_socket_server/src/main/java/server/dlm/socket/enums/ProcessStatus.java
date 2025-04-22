package server.dlm.socket.enums;

import lombok.Getter;

@Getter
public enum ProcessStatus {
    NONE("N"),
    SUCCESS("S");

    String value;

    ProcessStatus(String value) {
        this.value = value;
    }
}

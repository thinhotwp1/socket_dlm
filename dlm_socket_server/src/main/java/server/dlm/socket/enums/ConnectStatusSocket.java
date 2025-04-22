package server.dlm.socket.enums;

import lombok.Getter;

@Getter
public enum ConnectStatusSocket {
    CONNECTED("1"), DISCONNECTED("0");

    private String value;

    ConnectStatusSocket(String value){
        this.value = value;
    }
}

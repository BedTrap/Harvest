package me.eureka.harvest.events.event;

import me.eureka.harvest.events.Cancelled;


public class KeyEvent extends Cancelled {
    private int code;

    public KeyEvent(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public static class Freecam extends Cancelled {
        public int code;
        public int scanCode;
        public int action;
        public Freecam(int code, int scanCode, int action) {
            this.code = code;
            this.scanCode = scanCode;
            this.action = action;
        }
    }
}

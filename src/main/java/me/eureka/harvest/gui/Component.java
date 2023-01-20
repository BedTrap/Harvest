package me.eureka.harvest.gui;

public interface Component {
    void render(int mouseX, int mouseY);

    void mouseDown(int mouseX, int mouseY, int button);

    void mouseUp(int mouseX, int mouseY);

    void keyPress(int key);

    void close();
}
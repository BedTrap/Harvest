package me.eureka.harvest.ic;

import me.eureka.harvest.Hack;
import me.eureka.harvest.ic.util.Wrapper;
import me.eureka.harvest.systems.modules.Module;
import com.google.gson.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Configs extends Thread implements Wrapper {
    public static Configs get;
    public static final File mainFolder = new File("harvest");
    private static final String modulesFolder = mainFolder.getAbsolutePath() + "/modules";
    private static final String prefix = "prefix.json";
    private static final String friends = "friends.json";

    public Configs() {
        get = this;
    }

    public void load() {
        try {
            loadPrefix();
            loadModules();
            loadFriends();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadModules() throws IOException {
        for (Module m : Hack.modules().get()) {
            loadModule(m);
        }
    }

    private void loadModule(Module m) throws IOException {
        Path path = Path.of(modulesFolder, m.getName() + ".json");
        if (!path.toFile().exists()) return;
        String rawJson = loadFile(path.toFile());
        JsonObject jsonObject = JsonParser.parseString(rawJson).getAsJsonObject();

        if (jsonObject.get("Enabled") != null && jsonObject.get("Drawn") != null && jsonObject.get("Bind") != null) {
            if (jsonObject.get("Enabled").getAsBoolean()) m.toggle();
            m.setDrawn(jsonObject.get("Drawn").getAsBoolean());
            m.setBind(jsonObject.get("Bind").getAsInt());
        }

        Hack.settings().getSettingsForMod(m).forEach(s -> {
            JsonElement settingObject = jsonObject.get(s.getName());
            if (settingObject != null) {
                switch (s.getType()) {
                    case Boolean -> s.setValue(settingObject.getAsBoolean());
                    case Double -> s.setValue(settingObject.getAsDouble());
                    case Mode -> s.setValue(settingObject.getAsString());
                    case Integer -> s.setValue(settingObject.getAsInt());
                }
            }
        });
    }

    private void loadFriends() throws IOException {
        Path path = Path.of(mainFolder.getAbsolutePath(), friends);
        if (!path.toFile().exists()) return;
        String rawJson = loadFile(path.toFile());
        JsonObject jsonObject = new JsonParser().parse(rawJson).getAsJsonObject();
        if (jsonObject.get("friends") != null) {
            JsonArray friendObject = jsonObject.get("friends").getAsJsonArray();
            friendObject.forEach(object -> Hack.friends().friends().add(object.getAsString()));
        }
    }

    private void loadPrefix() throws IOException {
        Path path = Path.of(mainFolder.getAbsolutePath(), prefix);
        if (!path.toFile().exists()) return;
        String rawJson = loadFile(path.toFile());
        JsonObject jsonObject = new JsonParser().parse(rawJson).getAsJsonObject();

        if (jsonObject.get("prefix") != null) {
            Command.setPrefix(jsonObject.get("prefix").getAsString());
        }
    }

    public String loadFile(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file.getAbsolutePath());
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    @Override
    public void run() {
        if (!mainFolder.exists() && !mainFolder.mkdirs()) System.out.println("Failed to create config folder");
        if (!new File(modulesFolder).exists() && !new File(modulesFolder).mkdirs())
            System.out.println("Failed to create modules folder");
        try {
            saveModules();
            saveFriends();
            savePrefix();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveModules() throws IOException {
        for (Module m : Hack.modules().get()) {
            saveModule(m);
        }
    }

    private void saveModule(Module m) throws IOException {
        Path path = Path.of(modulesFolder, m.getName() + ".json");
        createFile(path);
        JsonObject jsonObject = new JsonObject();

        jsonObject.add("Enabled", new JsonPrimitive(m.isActive()));
        jsonObject.add("Drawn", new JsonPrimitive(m.isDrawn()));
        jsonObject.add("Bind", new JsonPrimitive(m.getBind()));
        Hack.settings().getSettingsForMod(m).forEach(s -> {
            switch (s.getType()) {
                case Mode -> jsonObject.add(s.getName(), new JsonPrimitive((String) s.get()));
                case Boolean -> jsonObject.add(s.getName(), new JsonPrimitive((Boolean) s.get()));
                case Double -> jsonObject.add(s.getName(), new JsonPrimitive((Double) s.get()));
                case Integer -> jsonObject.add(s.getName(), new JsonPrimitive((Integer) s.get()));
            }
        });
        Files.write(path, gson.toJson(new JsonParser().parse(jsonObject.toString())).getBytes());
    }

    private void saveFriends() throws IOException {
        Path path = Path.of(mainFolder.getAbsolutePath(), friends);
        createFile(path);
        JsonObject jsonObject = new JsonObject();
        JsonArray friends = new JsonArray();
        Hack.friends().friends().forEach(friends::add);
        jsonObject.add("friends", friends);
        Files.write(path, gson.toJson(new JsonParser().parse(jsonObject.toString())).getBytes());
    }

    private void savePrefix() throws IOException {
        Path path = Path.of(mainFolder.getAbsolutePath(), prefix);
        createFile(path);
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("prefix", new JsonPrimitive(Command.getPrefix()));
        Files.write(path, gson.toJson(new JsonParser().parse(jsonObject.toString())).getBytes());
    }

    private void createFile(Path path) {
        if (Files.exists(path)) new File(path.normalize().toString()).delete();
        try {
            Files.createFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

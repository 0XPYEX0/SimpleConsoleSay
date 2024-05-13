package me.xpyex.plugin.simpleconsolesay.bukkit;

import java.lang.reflect.Field;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleConsoleSay extends JavaPlugin {
    private static ClassLoader CLASS_LOADER;
    private static boolean HANDLING = false;

    @Override
    public void onEnable() {
        getLogger().info("启动中");
        CLASS_LOADER = getClassLoader();
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onServerCmd(ServerCommandEvent event) {
                if (HANDLING) {
                    return;
                }
                HANDLING = true;
                if (event.getCommand().startsWith("/")) {
                    event.setCancelled(true);
                    executeCmdSafely(event.getSender(), event.getCommand().substring(1));
                } else if (trace() == null) {  //如果trace()返回非空，那就是插件处理的
                    event.setCancelled(true);
                    executeCmdSafely(event.getSender(), "say " + event.getCommand());
                }
                HANDLING = false;
            }
        }, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("卸载中");
        // Plugin shutdown logic
    }

    private void executeCmdSafely(CommandSender sender, String cmd) {
        try {
            getServer().dispatchCommand(sender, cmd);
        } catch (Throwable e) {
            getLogger().info("出现错误: " + e);
            e.printStackTrace();
        }
    }

    public static Plugin trace() {
        try {
            StackTraceElement[] stackTrace = new Throwable().getStackTrace();
            // 从最前面找 最初的调用者
            for (int i = stackTrace.length - 1; i >= 0; i--) {
                try {
                    ClassLoader classLoader = Class.forName(stackTrace[i].getClassName()).getClassLoader();
                    // 不能找到自己
                    if (classLoader == null || classLoader.equals(CLASS_LOADER)) {
                        continue;
                    }

                    // 找这个 plugin 字段
                    Field pluginField = classLoader.getClass().getDeclaredField("plugin");
                    pluginField.setAccessible(true);
                    return (Plugin) pluginField.get(classLoader);
                } catch (ClassNotFoundException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (NoSuchFieldException | ClassCastException e) {
                    // 找不到plugin字段说明 不是 PluginClassLoader 类
                }

            }
        } catch (Throwable ignored) {}
        return null;
    }
}

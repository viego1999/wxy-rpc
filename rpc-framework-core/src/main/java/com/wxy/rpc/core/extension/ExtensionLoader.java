package com.wxy.rpc.core.extension;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 拓展加载器类 <p>
 * 参考：<a href="https://cn.dubbo.apache.org/zh/docsv2.7/dev/source/dubbo-spi/">dubbo spi</a>
 * <p>
 * Extension - 即实现类，每一个接口 com.xxx.XxxInterface 对应一个文件，一个 ExtensionLoader 对应一个 interface，Loader 存储了
 * 该接口的所有扩展类实现类，并且缓存了这个接口文件内所有的声明的实现类
 * </p>
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName ExtensionLoader
 * @Date 2023/1/11 18:44
 */
@Slf4j
public class ExtensionLoader<T> {

    /**
     * 服务的存储目录
     */
    private static final String SERVICES_DIRECTORY = "META-INF/extensions/";

    /**
     * 扩展类加载器缓存，key - class，val - 对应的扩展器类
     */
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();

    /**
     * 存储接口实现类的实例，key - impClass，val - object 实例对象
     */
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    /**
     * 拓展类加载器对应的接口类型
     */
    private final Class<?> type;

    /**
     * 扩展类工厂（实现依赖注入）
     */
    private final ExtensionFactory objectFactory;

    /**
     * 缓存的实例
     */
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();

    private final Holder<Object> cachedAdaptiveInstance = new Holder<>();

    /**
     * 缓存的类型（当前接口的所有 Extension 类型，对应文件内的：String - key，implClass - value）
     */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    private ExtensionLoader(Class<?> type) {
        this.type = type;
        objectFactory = null;
    }

    /**
     * 获取指定服务类型的拓展类加载器
     *
     * @param type 指定类型
     * @param <T>  服务类
     * @return 拓展类加载器
     */
    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException(String.format("Extension type (%s) is not an interface!", type));
        }
        if (type.getAnnotation(SPI.class) == null) {
            throw new IllegalArgumentException(String.format("Extension type (%s) is not an extension, " + "because it is NOT annotated with @%s!", type, SPI.class.getSimpleName()));
        }
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }

    /**
     * 根据指定的 key 值获取扩展实现类实例
     *
     * @param name 指定名称
     * @return 扩展实现类实例
     */
    @SuppressWarnings("unchecked")
    public T getExtension(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Extension name == null.");
        }
        // 先从缓存中取出对应实例 Holder
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }
        // 再从 Holder 中取出实例
        Object instance = holder.get();
        if (instance == null) {
            // 锁定当前的 holder 对象，此时 holder 为互斥资源
            synchronized (holder) {
                // 再次获取，防止已经被创建（单例模式的双从检查机制）
                instance = holder.get();
                if (instance == null) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * 根据 name （文件中定义的 key）创建 Extension 实例
     *
     * @param name 指定名称
     * @return 实例对象
     */
    @SuppressWarnings("unchecked")
    private T createExtension(String name) {
        // 获取指定 name 的拓展实现类类型
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw new IllegalArgumentException("No such extension name " + name);
        }
        // 获取对应类型的实例
        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        // 如果为空则通过反射机制创建一个
        if (instance == null) {
            try {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            } catch (InstantiationException | IllegalAccessException e) {
                log.error("Failed to create extension instance.", e);
            }
        }
        return instance;
    }

    /**
     * 获取当前接口的所有扩展实现类类型
     */
    private Map<String, Class<?>> getExtensionClasses() {
        // 先去 Holder 中读取当前接口的所有扩展实现类类型
        Map<String, Class<?>> classes = cachedClasses.get();
        // 如果为空，使用双从检查去加载并缓存
        if (classes == null) {
            // 锁定当前 cachedClasses的 holder（此时为互斥资源）
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = new HashMap<>();
                    // 去加载所有的扩展类从指定的扩展服务加载目录（MATA-INF/extensions）
                    loadDirectory(classes);
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    /**
     * 从指定的目录加载当前接口的所有拓展类
     *
     * @param extensionClasses 所有拓展类类型缓存 map
     */
    private void loadDirectory(Map<String, Class<?>> extensionClasses) {
        // 拼接文件名
        String filename = SERVICES_DIRECTORY + type.getName();
        try {
            Enumeration<URL> urls;
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();
            urls = classLoader.getResources(filename);
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL resourceUrl = urls.nextElement();
                    loadResource(extensionClasses, classLoader, resourceUrl);
                }
            }
        } catch (IOException e) {
            log.debug("Failed to load directory.", e);
        }
    }

    /**
     * 加载指定资源路径下的所有扩展类类型
     *
     * @param extensionClasses 扩展类类型缓存
     * @param classLoader      类加载器
     * @param resourceUrl      路径资源
     */
    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL resourceUrl) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream(), StandardCharsets.UTF_8))) {
            String line;
            // 按行读取
            while ((line = reader.readLine()) != null) {
                // 得到注释的第一个索引值
                final int ci = line.indexOf("#");
                if (ci >= 0) {
                    // 过滤注释，截取 # 号前面的内容
                    line = line.substring(0, ci);
                }
                line = line.trim();
                if (line.length() > 0) {
                    try {
                        // 找到 = 的第一个位置
                        final int i = line.indexOf("=");
                        String name = line.substring(0, i).trim();
                        String className = line.substring(i + 1).trim();
                        if (name.length() > 0 && className.length() > 0) {
                            Class<?> clazz = classLoader.loadClass(className);
                            extensionClasses.put(name, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.error("Failed to load extension class (interface: " + type + ", class line: " + line + ") in "
                                + resourceUrl + ", cause: " + e.getMessage(), e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Exception occurred when loading extension class (interface: " +
                    type + ", class file: " + resourceUrl + ") in " + resourceUrl, e);
            throw new RuntimeException(e);
        }
    }
}

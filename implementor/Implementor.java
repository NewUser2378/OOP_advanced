package info.kgeorgiy.ja.kupriyanov.implementor;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.nio.charset.*;

/**
 * implementing base class {@link JarImpler}
 */

public class Implementor implements JarImpler {
    /**
     * {@link String} to get name of class
     */
    private String name;
    /**
     * {@link String} to get package of class
     */
    private String packageName;
    /**
     * {@link StringBuilder} to store implementation of class
     */
    private StringBuilder builder;

    /**
     * {@link File} to store file of class
     */
    private File file;

    /**
     * {@link String} to get correct path on different OS
     */
    private final String separator = File.separator;
    /**
     * Constructs an Implementor.
     */
    public Implementor() {}


    /**
     * Add errors to {@link #builder}
     *
     * @param exceptions all exceptions in array
     */
    private void addThrowsClause(Class<?>[] exceptions) {
        if (exceptions.length != 0) {
            builder.append(" throws ");
        }
        int i=0;
        while ( i < exceptions.length) {
            builder.append(exceptions[i].getName());
            if (i != exceptions.length - 1) {
                builder.append(", ");
            }
            i++;
        }
    }

    /**
     * get return of methods to {@link #builder}
     *
     * @param returnType {@link Class} is return type of the method
     */
    private void addReturnStatement(Class<?> returnType) {
        if (returnType.equals(void.class)) {

        } else if (returnType.equals(boolean.class)) {
            builder.append("return true;");
        } else if (returnType.isPrimitive()) {
            builder.append("return 0;");
        } else {
            builder.append("return null;");
        }
    }
    /**
     * get method to {@link #builder}
     *
     * @param method {@link Method} is method that we need to add
     */
    private void generateMethod(Method method) {
        builder.append(Modifier.toString(method.getModifiers()).replace("abstract", "")
                        .replace("transient", ""))
                .append(" ").append(method.getReturnType().getTypeName())
                .append(" ").append(method.getName()).append("(");
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            builder.append(params[i].getCanonicalName()).append(" arg").append(i);
            if (i != params.length - 1) {
                builder.append(", ");
            }
        }
        builder.append(")");
        addThrowsClause(method.getExceptionTypes());
        builder.append(" {\n");
        addReturnStatement(method.getReturnType());
        builder.append("\n}\n");
    }


    /**
     * collecting all parameters to  {@link #builder}
     *
     * @param curInterface {@link Class} is class that we are getting
     * @throws ImplerException if we cant  is not Imply it as it not an interface
     */
    private void generateInterface(Class<?> curInterface) throws ImplerException {
        if (!Modifier.isInterface(curInterface.getModifiers())) {
            throw new ImplerException("No interface found");
        }

        if (Modifier.isPrivate(curInterface.getModifiers())) {
            throw new ImplerException("Error with private interface");
        }

        if (!packageName.isEmpty()) {
            builder.append("package ").append(packageName).append(";\n");
        }

        Method[] methods = curInterface.getMethods();

        builder.append("public class ").append(name).append(" implements ").append(curInterface.getCanonicalName()).append(" {\n");

        for (Method method : methods) {
            generateMethod(method);
        }

        builder.append("\n}");

        for (Class<?> nestedInterface : curInterface.getDeclaredClasses()) {
            if (nestedInterface.isInterface()) {
                generateInterface(nestedInterface);
            }
        }
    }
    /**
     * Implements the given interface and writes the result to a .java file.
     *
     * @param interfaceClass the interface to implement
     * @param outputPath     the path where the result class will be located
     * @throws ImplerException if interfaceClass couldn't be implemented
     */
    public void implement(Class<?> interfaceClass, Path outputPath) throws ImplerException {
        builder = new StringBuilder();
        packageName = (interfaceClass.getPackage() == null) ? "" : interfaceClass.getPackage().getName();
        name = interfaceClass.getSimpleName() + "Impl";

        file = new File(outputPath + separator + packageName.replace(".", separator) + separator + name + ".java");
        file.getParentFile().mkdirs();

        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new ImplerException("Error creating file", e);
        }
        generateInterface(interfaceClass);
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (final char c : builder.toString().toCharArray()) {
                writer.write(String.format("\\u%04x", (int) c));
            }
        } catch (IOException e) {
            throw new ImplerException("Error writing to file", e);
        }
    }

    /**
     * Implements the given interface and writes the result to a .jar file.
     *
     * @param interfaceClass the interface to implement
     * @param jarPath        the path where the result jar-file will be located
     * @throws ImplerException if interfaceClass couldn't be implemented
     */

    public void implementJar(Class<?> interfaceClass, Path jarPath) throws ImplerException {
        Path tempDir = Paths.get("." + separator + "tmp");
        implement(interfaceClass, tempDir);

        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            String[] compilerOptions = {"-classpath", "."};
            int result = compiler.run(null, null, null, "-classpath", compilerOptions[1], file.getPath());
            if (result != 0) {
                throw new ImplerException("Compilation failed");
            }
        } catch (NullPointerException e) {
            throw new ImplerException("Java compiler not found", e);
        } catch (Exception e) {
            throw new ImplerException("Error during compilation: " + e.getMessage(), e);
        }

        try (JarOutputStream jarOut = new JarOutputStream(Files.newOutputStream(jarPath))) {
            Files.walk(tempDir)
                    .filter(p -> p.toString().endsWith(".class"))
                    .forEach(p -> {
                        try (InputStream inputStream = Files.newInputStream(p)) {
                            Path relativePath = tempDir.relativize(p);
                            String entryName = relativePath.toString().replace(File.separator, "/");
                            jarOut.putNextEntry(new ZipEntry(entryName));
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                jarOut.write(buffer, 0, bytesRead);
                            }
                            jarOut.closeEntry();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            throw new ImplerException("Error while creating JAR: " + e.getMessage(), e);
        }
    }
    /**
     * The entry point of the program.
     *
     * @param args the arguments provided via command line, including the class name and optional arguments
     */
    public static void main(String[] args) {
        if (args == null) {
            System.err.println("Arguments cannot be null");
            return;
        }

        if (args.length < 2 || args.length > 3) {
            System.err.println("Only two or three arguments allowed");
            return;
        }
        Implementor implementor = new Implementor();
        try {
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            }
            if (args.length == 3) {
                if (args[0].equals("-jar")) {
                    implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
                } else {
                    System.err.println("No -jar argument found");
                }
            }
        } catch (InvalidPathException e) {
            System.out.println("Wrong path: " + ((args.length == 2) ? args[1] : args[2]));
        } catch (ImplerException e) {
            System.out.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println(((args.length == 2) ? args[0] : args[1]) + " Class not found");
        }
    }
}




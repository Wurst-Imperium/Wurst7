package net.wurstclient.util;

public enum ErrorUtil
{
    ;

    public static void runUnchecked(ExceptionThrowingFunction function)
    {
        try
        {
            function.run();
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void runUnchecked(ExceptionThrowingFunction function, String description)
    {
        try
        {
            function.run();
        } catch (Exception e)
        {
            throw new RuntimeException(description, e);
        }
    }

    @FunctionalInterface
    public interface ExceptionThrowingFunction
    {
        void run() throws Exception;
    }
}

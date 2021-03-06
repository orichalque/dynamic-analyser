package com.tblf.integration;

import com.tblf.classloading.SingleURLClassLoader;
import com.tblf.instrumentation.sourcecode.SourceCodeInstrumenter;
import com.tblf.linker.Calls;
import com.tblf.linker.tracers.FileTracer;
import com.tblf.parsing.TraceType;
import com.tblf.utils.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InstrumentationLinkageTest {

    @Before
    public void setup() throws IOException {
        File dir = new File("src/test/resources/sources/SimpleProject/");

        if (dir.exists())
            FileUtils.deleteDirectory(dir);

        com.tblf.utils.FileUtils.unzip(new File("src/test/resources/sources/SimpleProject.zip"));
        File file = new File(Configuration.getProperty("traceFile"));
        if (file.exists())
            FileUtils.forceDelete(file);

    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(new File("src/test/resources/sources/SimpleProject"));
    }

    @Test
    public void checkInstrument() throws IOException {
        Configuration.setProperty("trace", String.valueOf(TraceType.FILE));

        //Instrumenting the project
        File proj = new File("src/test/resources/sources/SimpleProject/");
        File binOut = new File(proj, "binOut");

        if (binOut.exists())
            FileUtils.deleteDirectory(binOut);

        Assert.assertTrue(proj.exists());
        SourceCodeInstrumenter instrumenter = new SourceCodeInstrumenter();
        instrumenter.setDirectory(proj);
        instrumenter.setBinDirectory(binOut);

        instrumenter.instrument(Collections.singletonList("com.tblf.SimpleProject.App"), Collections.singletonList("com.tblf.SimpleProject.AppTest"));

        //All classes have been compiled
        Assert.assertTrue(
                Files.walk((binOut.toPath())).filter(path -> path.toString().endsWith(".class")).collect(Collectors.toList()).size()
                        >= Files.walk(proj.toPath()).filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList()).size());

        try {
            //Running the class
            Calls.setTracer(new FileTracer());
            Calls.getTracer().startTrace();
            Class clazz = SingleURLClassLoader.getInstance().getClassLoader().loadClass("com.tblf.SimpleProject.AppTest");
            JUnitCore.runClasses(clazz);
        } catch (Throwable e) {
            Assert.fail(e.toString());
        }

        FileUtils.deleteDirectory(binOut);

        File file = new File(Configuration.getProperty("traceFile"));

        String result = IOUtils.toString(file.toURI(), "UTF-8");

        String oracle = "&:com.tblf.SimpleProject.AppTest:testApp\n" +
                        "%:com.tblf.SimpleProject.App:<init>\n" +
                        "!:89:130\n" +
                        "%:com.tblf.SimpleProject.App:method\n" +
                        "!:165:198\n" +
                        "!:202:240";

        List resultList = Arrays.asList(result.split("\n"));
        List oracleList = Arrays.asList(oracle.split("\n"));

        for (int i = 0; i < oracleList.size(); ++i) {
            Assert.assertEquals(oracleList.get(i), resultList.get(i));
        }
    }

}

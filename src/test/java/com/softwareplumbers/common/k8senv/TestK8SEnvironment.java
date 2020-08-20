/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.softwareplumbers.common.k8senv;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Test cases for K8SEnvironment.
 *
 * @author Jonathan Essex
 */
public class TestK8SEnvironment {

    Path root = Paths.get(System.getProperty("java.io.tmpdir"),"testK8S");
    
    public static final void createEntry(Path path, String data) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path)) {
            writer.write(data);
        }        
    }
    
    @Before
    public void setup() throws IOException {
        Files.createDirectory(root);
        createEntry(root.resolve("propertyA"), "valueA");
        createEntry(root.resolve("propertyB"), "valueB");
        Path subdir = root.resolve("propertyC");
        Files.createDirectory(subdir);        
        createEntry(subdir.resolve("propertyD"), "valueD");
    }
    
    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(root.resolve(Paths.get("propertyC", "propertyD")));
        Files.deleteIfExists(root.resolve(Paths.get("propertyC")));
        Files.deleteIfExists(root.resolve(Paths.get("propertyB")));
        Files.deleteIfExists(root.resolve(Paths.get("propertyA")));
        Files.deleteIfExists(root);
    }
    
    @Test
    public void testSecrets() {
        K8SEnvironment env = new K8SEnvironment(root);
        assertThat(env.getSecrets().get("propertyA"), equalTo("valueA"));
        assertThat(env.getSecrets().get("propertyB"), equalTo("valueB"));
        Map<String,Object> propertyC = (Map<String,Object>)env.getSecrets().get("propertyC");
        assertThat(propertyC.get("propertyD"), equalTo("valueD"));
    }
    
}

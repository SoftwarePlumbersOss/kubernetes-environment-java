/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.softwareplumbers.common.k8senv;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A bean representing configuration information typically passed by Kubernetes into a container.
 *
 * @author Jonathan Essex
 */
public class K8SEnvironment {
    
    private static final Logger LOG = LoggerFactory.getLogger(K8SEnvironment.class);
   
    private Path secretsPath;
    
    /** Create a new Kubernetes environment.
     * 
     * @param secretsPath Path on which Kubernetes secrets can be found.
     */
    public K8SEnvironment(Path secretsPath) {
        this.secretsPath = secretsPath;
    }
    
    /** Create a new Kubernetes environment.
     * 
     * No-arg constructor for Spring. 
     * 
     */
    public K8SEnvironment() {
        this(null);
    }
    
    /** Set the path on which Kubernetes secrets can be found.
     * 
     * @param path Path (appropriately formatted for the underlying O/S) to secrets. 
     */
    public void setSecretsPath(String path) {
        secretsPath = Paths.get(path);
    }

    /** Set the path on which Kubernetes secrets can be found.
     * 
     * @param parts Array of path segments which will be concatenated to create secrets path.
     */
    public void setSecretsPathParts(String... parts) {
        secretsPath = Paths.get(parts[0], Arrays.copyOfRange(parts, 1, parts.length));
    }
    
    private static class DirectoryMap implements Map<String, Object> {

        private final Path root;
        
        public DirectoryMap(Path root) {
            this.root = root;
        }

        @Override
        public int size() {
            try {
                return (int)Files.list(root).count();
            } catch (IOException ioe) {
                return 0;
            }
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public boolean containsKey(Object key) {
            return Files.exists(root.resolve(key.toString()));
        }

        @Override
        public boolean containsValue(Object value) {
            return values().contains(value);
        }

        @Override
        public Object get(Object key) {
            Path path = root.resolve(key.toString());
            if (Files.exists(path)) {
                Object result = toValue(path);
                return result;
            } else {
                return null;
            }
        }

        @Override
        public Object put(String key, Object value) {
            throw new UnsupportedOperationException("Directory map is read only"); 
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException("Directory map is read only"); 
        }

        @Override
        public void putAll(Map<? extends String, ? extends Object> m) {
            throw new UnsupportedOperationException("Directory map is read only"); 
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Directory map is read only"); 
        }

        @Override
        public Set<String> keySet() {
            try {
                return Files.list(root).map(path->path.getFileName().toString()).collect(Collectors.toSet());
            } catch (IOException ioe) {
                return Collections.emptySet();
            }
        }
        
        private static Object toValue(Path path) {
            if (Files.isDirectory(path)) {
                return new DirectoryMap(path);
            } else {
                try {
                    return new String(Files.readAllBytes(path)).trim();
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }            
        }
        
        private static String toKey(Path path) {
            return path.getFileName().toString();
        }

        @Override
        public Collection<Object> values() {
            try {
                return Files.list(root).map(DirectoryMap::toValue).collect(Collectors.toSet());
            } catch (IOException ioe) {
                return Collections.emptySet();
            }
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            try {
                return Files.list(root).collect(Collectors.toMap(DirectoryMap::toKey, DirectoryMap::toValue)).entrySet();
            } catch (IOException ioe) {
                return Collections.emptySet();
            }
        }        
    }
    
    /** Get the Kubernetes secrets as a map.
     * 
     * Exposes the content of the secrets directory as a map. Each map entry
     * represents a file or subdirectory on the secrets path. If a file, the
     * mapped value is a string containing the file content. If a directory,
     * the mapped value is a Map exposing the content of that directory.
     * 
     * Note that if the K8SEnvironment object is a spring bean named K8SEnv, then
     * the SPEL expression #{@K8SEnv.secrets['SECRET_KEY']} can be used to access
     * the secret 'SECRET_KEY' provided by Kubernetes.
     * 
     * @return the secrets map.
     */
    public Map<String, Object> getSecrets() {
        return new DirectoryMap(secretsPath);
    }
}

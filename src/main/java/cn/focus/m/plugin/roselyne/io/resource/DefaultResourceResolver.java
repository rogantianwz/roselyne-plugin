package cn.focus.m.plugin.roselyne.io.resource;

import cn.focus.m.plugin.roselyne.Message;
import cn.focus.m.plugin.roselyne.descriptor.Config;
import cn.focus.m.plugin.roselyne.descriptor.SourceFile;
import cn.focus.m.plugin.roselyne.descriptor.SourceFileSet;
import cn.focus.m.plugin.roselyne.descriptor.SourceFilesExcludes;
import cn.focus.m.plugin.roselyne.descriptor.SourceFilesIncludes;
import cn.focus.m.plugin.roselyne.utils.RoselyneFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.StringUtils;

public class DefaultResourceResolver implements ResourceResolver {
    
    private ResourceAcquirerFactory resourceAcquirerFactory;

    public DefaultResourceResolver() {
        resourceAcquirerFactory = new ResourceAcquirerFactory();
    }
    
    public DefaultResourceResolver(ResourceAcquirerFactory resourceAcquirerFactory) {
        this.resourceAcquirerFactory = resourceAcquirerFactory;
    }

    public List<Resource> resolve(Config config, ResourceResolveSupport support) throws ResourceResolveException {
        List<Resource> resources = new ArrayList<Resource>();
        try {
            resolveFileSets(config, support, resources);
            resolveFiles(config, support, resources);
        } catch (ResourceInvalidException e) {
            throw new ResourceResolveException(e);
        } catch (ResourceNotFoundException e) {
            throw new ResourceResolveException(e);
        } catch (IOException e) {
            throw new ResourceResolveException(e);
        }
        return resources;
    }
    
    /**
     * 从config中获取所有FileSet的资源(只获取resolveImmeditely=true的资源)
     * @param config
     * @param support
     * @param resources
     * @return
     * @throws ResourceResolveException
     * @throws ResourceNotFoundException 
     * @throws ResourceInvalidException 
     * @throws IOException 
     */
    protected void resolveFileSets(Config config, ResourceResolveSupport support, 
            List<Resource> resources) throws ResourceResolveException, ResourceInvalidException, ResourceNotFoundException, IOException {
        List<SourceFileSet> fileSets = config.getFileSets();
        if (null == fileSets) {
            return;
        }
        
        for (SourceFileSet fileSet : fileSets) {
            if (!fileSet.isResolveImmeditely()) {
                continue;
            }
            List<Resource> resolvedResources = resolveFileSet(fileSet, support);
            if (null != resolvedResources && resolvedResources.size() > 0) {
                resources.addAll(resolvedResources);
            } else {
                //TODO:add some log
            }
        }
    }
    
    /**
     * 从config中获取所有files的资源
     * @param config
     * @param support
     * @param resources
     * @throws ResourceInvalidException
     * @throws ResourceNotFoundException
     * @throws IOException 
     */
    protected void resolveFiles(Config config, ResourceResolveSupport support, 
            List<Resource> resources) throws ResourceInvalidException, ResourceNotFoundException, IOException {
        List<SourceFile> files = config.getFiles();
        if (null == files || files.size() == 0) {
            return;
        }
        for (SourceFile sourceFile : files) {
            Resource resource = resolveSourceFile(sourceFile, support);
            if (null != resource) {
                resources.add(resource);
            } else {
                //TODO:add some log
            }
        }
        
    }
    
    
    
    /**
     * 获取单个FileSet的资源
     * @param fileSet
     * @param support
     * @return
     * @throws ResourceNotFoundException 
     * @throws ResourceInvalidException 
     * @throws ResourceResolveException 
     * @throws IOException 
     */
    protected List<Resource> resolveFileSet(SourceFileSet fileSet, ResourceResolveSupport support) 
            throws ResourceInvalidException, ResourceNotFoundException, ResourceResolveException, IOException {
        List<Resource> resources = new ArrayList<Resource>();
        String directory = fileSet.getDirectory();
        if (StringUtils.isNotBlank(directory)) {
            File dir = new File(directory);
            if (!dir.exists() || !dir.isDirectory()) {
                throw new ResourceResolveException("dir：" + dir + " is not valid");
            }
            acquireFiles(dir,fileSet, null, resources, support);
        }
        return resources;
    }
    
    /**
     * 递归获取某个目录下的所有资源
     * @param dir
     * @param includes
     * @param excludes
     * @param fileList
     * @param support
     * @throws ResourceInvalidException 
     * @throws ResourceNotFoundException 
     * @throws IOException 
     */
    protected void acquireFiles(File dir, SourceFileSet fileSet, ResourceMatcher matcher,
            List<Resource> resourceList, ResourceResolveSupport support) throws ResourceInvalidException, ResourceNotFoundException, IOException {
        File[] files = dir.listFiles();
        if (null == files || files.length == 0) {
            return;
        }
        
        if (null == matcher) {
            matcher = new ResourceMatcher(fileSet);
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                acquireFiles(file, fileSet, matcher, resourceList, support);
            } else {
                    ResourceMatchResult matchedResult = matcher.matchFile(file);
                    if (!matchedResult.matched) {
                        continue;
                    }
                    ResourceAcquirer acquirer = resourceAcquirerFactory.getResourceAcquirer(file.getAbsolutePath());
                    
                    if (null != acquirer) {
                        File tempFile = acquirer.acquireResource(file.getAbsolutePath(), support.getTempDir());
                        Resource r = new Resource();
                        r.setSourceAddr(file.getCanonicalPath());
                        r.setTempAddr(tempFile.getCanonicalFile());
                        String releaseAddr = new String(fileSet.getRelease());
                        r.setReleaseAddr(releaseAddr);
                        String outputAddr = new String(matchedResult.getOutput());
                        r.setOutputAddr(new File(outputAddr).getCanonicalPath());
                        r.setMd5(fileSet.isMd5());
                        r.setMd5len(fileSet.getMd5len());
                        resourceList.add(r);
                    } else {
                        throw new ResourceInvalidException("Can not get ResourceAcquirer for [" + file.getAbsolutePath() + "]");
                    }
                
            }
        }
    }
    
    protected Resource resolveSourceFile(SourceFile sourceFile, ResourceResolveSupport support) throws ResourceInvalidException, ResourceNotFoundException, IOException {
        File file = new File(sourceFile.getSource());
        ResourceAcquirer acquirer = resourceAcquirerFactory.getResourceAcquirer(file.getAbsolutePath());
        
        if (null != acquirer) {
            File tempFile = acquirer.acquireResource(file.getAbsolutePath(), support.getTempDir());
            Resource r = new Resource();
            r.setSourceAddr(file.getCanonicalPath());
            r.setTempAddr(tempFile.getCanonicalFile());
            r.setReleaseAddr(new String(sourceFile.getRelease()));
            r.setOutputAddr(new File(sourceFile.getOutput()).getCanonicalPath());
            r.setMd5(sourceFile.isMd5());
            r.setMd5len(sourceFile.getMd5len());
            return r;
        } else {
            throw new ResourceInvalidException("Can not get ResourceAcquirer for [" + file.getAbsolutePath() + "]");
        }
    }
    
    /**
     * 测试reader
     * @param args
     * @throws IOException 
     */
    public static void main1(String[] args) throws IOException {
        DefaultResourceResolver r = new DefaultResourceResolver();
        File dir = new File("D:\\workspace1\\.\\test\\src\\main\\webapp\\views");
        List<Resource> resourceList = new ArrayList<Resource>();
        
        try {
            r.acquireFiles(dir, null, null, resourceList, new ResourceResolveSupportAdaptor(){

                public String getTempDir() {
                    return "D:\\workspace1\\test\\target\\fis\\temp";
                }

                public void addMessage(MessageType type, String content) {
                }

                public String getEncoding() {
                    return "UTF-8";
                }

                public void printResolveLog(Message msg) {
                    // TODO Auto-generated method stub
                    
                }
                
            });
            for (Resource resource : resourceList) {
                System.out.println(resource);
            }
        } catch (ResourceInvalidException e) {
            e.printStackTrace();
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 资源匹配器，根据fileSet的include和exclude配置匹配directory下的资源
     * @author rogantian
     *
     */
    public class ResourceMatcher {

        private List<Pattern> includePatterns = new ArrayList<Pattern>();

        private List<Pattern> excludePatterns = new ArrayList<Pattern>();
        
        private String output;
        
        private boolean outputSameAsSource = true;
        
        private File dir;
        
        public final ResourceMatchResult NoneMatchedResult = new ResourceMatchResult(false);
        
        public final ResourceMatchResult MatchedResult = new ResourceMatchResult(true);

        public ResourceMatcher(SourceFileSet fileSet) {
            output = fileSet.getOutpout();
            if (StringUtils.isNotBlank(output)) {
                outputSameAsSource = false;
            }
            dir = new File(fileSet.getDirectory());
            init(fileSet);
        }
        
        private void init(SourceFileSet fileSet) {
            SourceFilesIncludes filesIncludes = fileSet.getIncludes();

            if (null != filesIncludes) {
                List<String> includes = filesIncludes.getInclude();
                if (null != includes) {
                    for (String include : includes) {
                        Pattern p = Pattern.compile(include);
                        includePatterns.add(p);
                    }
                }
            }

            SourceFilesExcludes filesExcludes = fileSet.getExcludes();

            if (null != filesExcludes) {
                List<String> excludes = filesExcludes.getExclude();
                if (null != excludes) {
                    for (String exclude : excludes) {
                        Pattern p = Pattern.compile(exclude);
                        excludePatterns.add(p);
                    }
                }
            }
        }
        
        public ResourceMatchResult matchFile(File file) {
            
            String relativePath = RoselyneFileUtils.makePathRelativeTo(file.getAbsolutePath(), dir);
            
            /*System.out.println("dir:" + dir.getAbsolutePath());
            System.out.println("relativePath:" + relativePath);
            System.out.println("p:" + includePatterns.get(0).toString());*/
            
            Pattern matchedPattern = null;
            
            for (Pattern p : includePatterns) {
                if (p.matcher(relativePath).matches()) {
                    matchedPattern = p;
                }
            }
            
            if (null == matchedPattern) {
                return NoneMatchedResult;
            }
            
            for (Pattern p : excludePatterns) {
                if (p.matcher(relativePath).matches()) {
                    return NoneMatchedResult;
                }
            }
            
            if (outputSameAsSource) {
                return MatchedResult;
            } else {
                Matcher m = matchedPattern.matcher(relativePath);
                String newOutput = m.replaceAll(output);
                return new ResourceMatchResult(true, newOutput);
            }
        }
        
        
    }
    
    /**
     * 资源匹配结果
     * @author rogantian
     *
     */
    public class ResourceMatchResult {
        private boolean matched;

        private String output;

        
        public ResourceMatchResult(boolean matched) {
            super();
            this.matched = matched;
        }
        
        public ResourceMatchResult(boolean matched, String output) {
            super();
            this.matched = matched;
            this.output = output;
        }

        public boolean isMatched() {
            return matched;
        }

        public void setMatched(boolean matched) {
            this.matched = matched;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }

        @Override
        public String toString() {
            return "ResourceMatchResult [matched=" + matched + ", output=" + output + "]";
        }
    }
    
    /**
     * 测试filter
     * @param args
     */
    public static void main(String[] args) {
        String include = "(.*?)\\.jsp";
        List<String> includes = new ArrayList<String>();
        includes.add(include);
        SourceFilesIncludes filesIncludes = new SourceFilesIncludes();
        filesIncludes.setInclude(includes);
        
        String exclude = "home\\.jsp";
        List<String> excludes = new ArrayList<String>();
        excludes.add(exclude);
        SourceFilesExcludes filesExcludes = new SourceFilesExcludes();
        filesExcludes.setExclude(excludes);
        
        String directory = "D:/workspace1/./test/src/main/webapp/views";
        
        String output = "D:/workspace1/test/target/fis/temp/output/$1.jsp";
        
        SourceFileSet fileSet = new SourceFileSet();
        fileSet.setDirectory(directory);
        fileSet.setOutpout(output);
        fileSet.setIncludes(filesIncludes);
        //fileSet.setExcludes(filesExcludes);
        
        File file = new File("D:\\workspace1\\.\\test\\src\\main\\webapp\\views\\home.jsp");
        
        DefaultResourceResolver resolver = new DefaultResourceResolver();
        
        ResourceMatcher resourceMatcher = resolver.new ResourceMatcher(fileSet);
        ResourceMatchResult  matchResult = resourceMatcher.matchFile(file);
        
        System.out.println("matchResult is: " + matchResult);
    }

    public List<MagicResource> resolveMagic(Config fisConfig, ResourceResolveSupport support)
            throws ResourceResolveException {
        List<MagicResource> resources = new ArrayList<MagicResource>();
        List<SourceFileSet> fileSets = fisConfig.getFileSets();
        if (null != fileSets) {
            for (SourceFileSet fileSet : fileSets) {
                if (fileSet.isResolveImmeditely()) {
                    continue;
                }
                MagicResource mr = new MagicResource();
                String output = fileSet.getOutpout();
                if (StringUtils.isNotBlank(output)) {
                    try {
                        mr.setOutputAddr(new File(output).getCanonicalPath());
                    } catch (IOException e) {
                        throw new ResourceResolveException(e);
                    }
                }
                
                String release = fileSet.getRelease();
                if (StringUtils.isNotBlank(release)) {
                    mr.setReleaseAddr(new String(release));
                }
                
                String directory = fileSet.getDirectory();
                if (StringUtils.isBlank(directory)) {
                    directory = "";
                }
                
                SourceFilesIncludes fileIncludes = fileSet.getIncludes();
                if (null != fileIncludes) {
                    List<String> includes = fileIncludes.getInclude();
                    if (null != includes) {
                        List<Pattern> ps = new ArrayList<Pattern>();
                        for (String include : includes) {
                            Pattern p = Pattern.compile(directory + include);
                            ps.add(p);
                        }
                        mr.setIncludePatterns(ps);
                    }
                }
                
                SourceFilesExcludes  fileExcludes = fileSet.getExcludes();
                if (null != fileExcludes) {
                    List<String> excludes = fileExcludes.getExclude();
                    if (null != excludes) {
                        List<Pattern> ps = new ArrayList<Pattern>();
                        for (String exclude : excludes) {
                            Pattern p = Pattern.compile(directory + exclude);
                            ps.add(p);
                        }
                        mr.setExcludePatterns(ps);
                    }
                }
                mr.setMd5(fileSet.isMd5());
                mr.setMd5len(fileSet.getMd5len());
                resources.add(mr);
            }
        }
        return resources;
    }

}

    

package com.gphw.spring.framework.servlet;

import com.gphw.spring.framework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.net.URL;

public class GphwDispatcherServlet extends HttpServlet {
    private Properties config = new Properties();
    private Map<String, Object> ioc = new HashMap<String, Object>();
    private List<String> classNames = new ArrayList<String>();
    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        resp.getWriter().write("Servlet Invoke Success!");
//        super.doPost(req, resp);
        this.doDispatch(req, resp);
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {
        String url = req.getRequestURI();
        String contextPath=req.getContextPath();
        url=url.replaceAll(contextPath,"").replace("/+","/");
        if(!handlerMapping.containsKey(url)){
            try {
                resp.getWriter().write("404! Not Found!");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Map<String,String[]>  params = req.getParameterMap();

        Method method = handlerMapping.get(url);

        Class<?>[] parameterTypes = method.getParameterTypes();
        Object [] paramValues = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class paramType= parameterTypes[i];
            if(paramType==HttpServletRequest.class){
                paramValues[i]=req;
                continue;
            }else if(paramType==HttpServletResponse.class){
                paramValues[i]=resp;
                continue;
            }else if(paramType==String.class){
                Annotation[] an[] = method.getParameterAnnotations();
                for(int j=0;j<an.length;j++){
                    for(Annotation a:an[j]){
                        if(a instanceof GphwRequestParam){
                            String paramName = ((GphwRequestParam) a).value();
                            if(!"".equals(paramName.trim())){
                                String value = Arrays.toString(params.get(paramName))
                                        .replaceAll("\\[|\\]","")
                                        .replaceAll("\\s",",");
                                paramValues[i] = value;

                            }
                        }
                    }
                }
            }
            String beanName =this.toLowerCase( method.getDeclaringClass().getSimpleName());
            Object obj = ioc.get(beanName);
            try {
                method.invoke(obj,paramValues);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        doLoadConfig(servletConfig.getInitParameter("contextConfigLocation"));
        doScanner(config.getProperty("scannerPackage"));
        doInstance();
        doAutowired();
        initHandlerMapping();
        System.out.println("Gphw DispatcherServlet Inited!");
    }


    /**
     * 加载配置文件
     *
     * @param contextConfigLocation
     */
    private void doLoadConfig(String contextConfigLocation) {
        InputStream fis = null;
        try {
            fis = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
            config.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 扫描初始化bean的包
     *
     * @param scannerPackage
     */
    private void doScanner(String scannerPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scannerPackage.replaceAll("\\.", "/"));
        File file = new File(url.getFile());
        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                doScanner(scannerPackage + "."+f.getName());
            } else {
                if (!f.getName().endsWith(".class")) {
                    continue;
                }
                String className = (scannerPackage + "."+f.getName()).replace(".class", "");
                classNames.add(className);
            }
        }
    }

    /**
     * 实例化bean
     */
    private void doInstance() {
        for (String calssName : classNames) {
            try {
                Class<?> clazz = Class.forName(calssName);
                if (clazz.isAnnotationPresent(GphwController.class)) {
                    Object object = clazz.newInstance();
                    String beanName = toLowerCase(clazz.getSimpleName());
                    ioc.put(beanName, object);
                } else if (clazz.isAnnotationPresent(GphwService.class)) {
                    Object object = clazz.newInstance();
                    String beanName = toLowerCase(clazz.getSimpleName());
                    GphwService gphwService = clazz.getAnnotation(GphwService.class);
                    String annoServiceValue = gphwService.value();
                    if (null != annoServiceValue && !"".equalsIgnoreCase(annoServiceValue)) {
                        beanName = annoServiceValue;
                    }
                    ioc.put(beanName, object);
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (ioc.containsKey(i.getName())) {
                            continue;
                        }
                        ioc.put(i.getName(), object);
                    }
                } else {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 自动装配
     */
    private void doAutowired() {
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field fields[] = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(GphwAutowired.class)) {
                }
                GphwAutowired autowired = field.getAnnotation(GphwAutowired.class);
                String beanName = autowired.value();
                if (null == beanName || "".equalsIgnoreCase(beanName)) {
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 初始化映射关系
     */
    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(GphwController.class)) {
                continue;
            }else{
                GphwRequestMapping requestMapping = clazz.getAnnotation(GphwRequestMapping.class);
                String classMapping = requestMapping.value();
                Method methods[]=clazz.getDeclaredMethods();
                for(Method method:methods){
                    if(!method.isAnnotationPresent(GphwRequestMapping.class)){continue;}
                    GphwRequestMapping methodRequestMapping = method.getAnnotation(GphwRequestMapping.class);
                    String mappingPath = ("/"+classMapping+"/"+methodRequestMapping.value()).replaceAll("/+","/");
                    handlerMapping.put(mappingPath,method);
                    System.out.println("Mapped " + mappingPath + "," + method);
                }
            }
        }
    }

    /**
     * 首字母小写
     *
     * @param simpleName
     * @return
     */
    private String toLowerCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

}

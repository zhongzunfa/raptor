package com.ppdai.framework.raptor.codegen.core.swagger.tool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.DescriptorProtos;
import com.ppdai.framework.raptor.codegen.core.service2interface.CommandProtoc;
import com.ppdai.framework.raptor.codegen.core.swagger.container.EnumContainer;
import com.ppdai.framework.raptor.codegen.core.swagger.template.SwaggerTemplate;
import com.ppdai.framework.raptor.codegen.core.swagger.swaggerobject.SwaggerObject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by zhangyicong on 18-2-27.
 */
public class Proto2SwaggerJson {

    private static final Logger logger = LoggerFactory.getLogger(Proto2SwaggerJson.class);

    private final String apiVersion;

    private final String discoveryRoot;

    private final String generatePath;

    private final CommandProtoc commandProtoc;

    private final SwaggerTemplate swaggerTemplate = new SwaggerTemplate();

    private final ObjectMapper mapper;

    private Proto2SwaggerJson(String discoveryRoot, String generatePath,
                              final File protocDependenciesPath, String apiVersion) {

        this.discoveryRoot = discoveryRoot;
        this.generatePath = generatePath;
        this.commandProtoc =
                CommandProtoc.configProtoPath(discoveryRoot, protocDependenciesPath);
        this.mapper = new ObjectMapper();
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.apiVersion = apiVersion;
    }

    public static Proto2SwaggerJson forConfig(String discoveryRoot, String generatePath,
                                              final File protocDependenciesPath, String apiVersion) {
        return new Proto2SwaggerJson(discoveryRoot, generatePath, protocDependenciesPath, apiVersion);
    }

    public void generateFile(String protoPath) {
        logger.info("    Processing : " + protoPath);

        if (!new File(protoPath).exists()) {
            logger.warn("protoPath:" + protoPath + " not exist, it may be in the third party jars, so it can't be generate.java");
            return;
        }

        DescriptorProtos.FileDescriptorSet fileDescriptorSet = commandProtoc.invoke(protoPath);

        for (DescriptorProtos.FileDescriptorProto fdp : fileDescriptorSet.getFileList()) {
            //No service has been defined.
            if (fdp.getServiceCount() == 0) {
                logger.info(fdp.getName() + " seems to has no Service defined.");
                // 解析enum和message到container
                ContainerUtil.getEnumContainer(fdp);
                ContainerUtil.getMessageContainer(fdp);
                continue;
            }

            SwaggerObject swaggerObject = swaggerTemplate.applyTemplate(fdp, apiVersion);

            try {
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(swaggerObject);
                File apiFile = new File(generatePath + File.separatorChar + fdp.getName() + ".json");
                if (apiFile.exists()) {
                    apiFile.delete();
                }
                FileUtils.writeStringToFile(apiFile, json);

                //logger.info("Swagger API: {}", mapper.writeValueAsString(swaggerObject));
                logger.info("Generate Swagger API file: {}", apiFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

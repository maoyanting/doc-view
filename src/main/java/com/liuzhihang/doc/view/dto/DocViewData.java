package com.liuzhihang.doc.view.dto;

import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.utils.VelocityUtils;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DocView 的模版 用来使用 Velocity 生成内容
 * <p>
 * Velocity 会根据 get 方法 获取值, 不提供 set 方法
 *
 * @author liuzhihang
 * @date 2020/11/21 16:39
 */
@Data
public class DocViewData {

    /**
     * 文档名称
     */
    private final String name;

    /**
     * 文档描述
     */
    private final String desc;

    /**
     * 环境地址
     */
    // private final   String domain;

    /**
     * 接口地址
     */
    private final String path;

    /**
     * 请求方式 GET POST PUT DELETE HEAD OPTIONS PATCH
     */
    private final String method;


    /**
     * headers
     */
    private final List<DocViewParamData> requestHeaderDataList;

    private final String requestHeader;

    /**
     * 请求参数
     */
    private final List<DocViewParamData> requestDocViewParamDataList;

    private final String requestParam;


    /**
     * 请求参数
     */
    private final List<DocViewParamData> requestBodyDataList;

    private final String requestBody;

    /**
     * 请求示例
     */
    private final String requestExample;

    /**
     * 返回参数
     */
    private final List<DocViewParamData> responseDocViewParamDataList;
    private final String responseParam;


    /**
     * 返回示例
     */
    private final String responseExample;

    private final String type;

    public DocViewData(DocView docView) {

        this.name = docView.getName();
        this.desc = docView.getDesc();
        this.path = docView.getPath();
        this.method = docView.getMethod();
        this.type = docView.getType();

        this.requestHeaderDataList = headerDataList(docView.getHeaderList());
        this.requestHeader = headerMarkdown(requestHeaderDataList);

        this.requestDocViewParamDataList = paramDataList(docView.getReqParamList());
        this.requestParam = paramMarkdown(requestDocViewParamDataList);

        this.requestBodyDataList = buildBodyDataList(docView.getReqRootBody().getChildList());
        this.requestBody = paramMarkdown(requestBodyDataList);
        this.requestExample = buildReqExample(docView.getReqExampleType(), docView.getReqExample());

        this.responseDocViewParamDataList = buildBodyDataList(docView.getRespRootBody().getChildList());
        this.responseParam = paramMarkdown(responseDocViewParamDataList);
        this.responseExample = buildRespExample(docView.getReqExampleType(), docView.getRespExample());

    }

    @NotNull
    @Contract("_ -> new")
    public static DocViewData getInstance(@NotNull DocView docView) {
        return new DocViewData(docView);
    }

    public static String markdownText(Project project, DocView docView) {

        DocViewData docViewData = new DocViewData(docView);

        if (docView.getType().equalsIgnoreCase("Dubbo")) {
            return VelocityUtils.convert(TemplateSettings.getInstance(project).getDubboTemplate(), docViewData);
        } else {
            // 按照 Spring 模版
            return VelocityUtils.convert(TemplateSettings.getInstance(project).getSpringTemplate(), docViewData);
        }
    }

    /**
     * dataList 转为 Markdown 文本
     *
     * @param dataList
     * @return
     */
    @NotNull
    private static String paramMarkdown(List<DocViewParamData> dataList) {

        if (CollectionUtils.isEmpty(dataList)) {
            return "";
        }

        return "|参数名|类型|必选|描述|\n" +
                "|:-----|:-----|:-----|:-----|\n" +
                paramMarkdownContent(dataList);
    }

    /**
     * 表格内数据
     */
    private static StringBuilder paramMarkdownContent(List<DocViewParamData> dataList) {

        StringBuilder builder = new StringBuilder();

        for (DocViewParamData data : dataList) {
            builder.append("|").append(data.getPrefix()).append(data.getName())
                    .append("|").append(data.getType())
                    .append("|").append(data.getRequired() ? "Y" : "N")
                    .append("|").append(data.getDesc())
                    .append("|").append("\n");
            if (CollectionUtils.isNotEmpty(data.getChildList())) {
                builder.append(paramMarkdownContent(data.getChildList()));
            }
        }

        return builder;
    }


    @NotNull
    private static String headerMarkdown(List<DocViewParamData> dataList) {


        if (CollectionUtils.isEmpty(dataList)) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (DocViewParamData data : dataList) {
            builder.append("|").append(data.getName())
                    .append("|").append(data.getExample())
                    .append("|").append(data.getRequired() ? "Y" : "N")
                    .append("|").append(data.getDesc())
                    .append("|").append("\n");
        }


        return "|参数名|参数值|必填|描述|\n" +
                "|:-----|:-----|:-----|:-----|\n" +
                builder;
    }


    private List<DocViewParamData> headerDataList(List<Header> headerList) {

        if (CollectionUtils.isEmpty(headerList)) {
            return new ArrayList<>();
        }

        return headerList.stream().map(header -> {

            DocViewParamData data = new DocViewParamData();
            data.setPsiElement(header.getPsiElement());
            data.setName(header.getName());
            data.setExample(header.getValue());
            data.setRequired(header.getRequired());
            data.setDesc(StringUtils.isNotBlank(header.getDesc()) ? header.getDesc() : "");

            return data;
        }).collect(Collectors.toList());
    }

    private List<DocViewParamData> paramDataList(List<Param> reqParamList) {
        if (CollectionUtils.isEmpty(reqParamList)) {
            return new ArrayList<>();
        }

        return reqParamList.stream().map(param -> {

            DocViewParamData data = new DocViewParamData();
            data.setPsiElement(param.getPsiElement());
            data.setExample(param.getExample());
            data.setName(param.getName());
            data.setRequired(param.getRequired());
            data.setType(param.getType());
            data.setDesc(StringUtils.isNotBlank(param.getDesc()) ? param.getDesc() : "");

            return data;
        }).collect(Collectors.toList());
    }

    /**
     * 根据 bodyList 构建参数集合
     *
     * @param bodyList
     * @return
     */
    @NotNull
    public static List<DocViewParamData> buildBodyDataList(List<Body> bodyList) {

        if (CollectionUtils.isEmpty(bodyList)) {
            return new ArrayList<>();
        }

        return buildBodyDataList(bodyList, "");
    }


    /**
     * 递归 body 生成 List<ParamData>
     *
     * @param bodyList
     * @param prefix
     */
    @NotNull
    private static List<DocViewParamData> buildBodyDataList(@NotNull List<Body> bodyList, String prefix) {

        List<DocViewParamData> dataList = new ArrayList<>();

        for (Body body : bodyList) {

            DocViewParamData data = new DocViewParamData();
            data.setPsiElement(body.getPsiElement());
            data.setName(body.getName());
            data.setExample(body.getExample());
            data.setRequired(body.getRequired());
            data.setType(body.getType());
            data.setDesc(StringUtils.isNotBlank(body.getDesc()) ? body.getDesc() : "");

            data.setPrefix(prefix);

            if (CollectionUtils.isNotEmpty(body.getChildList())) {
                data.setChildList(buildBodyDataList(body.getChildList(), prefix + "-->"));
            }
            dataList.add(data);
        }
        return dataList;
    }


    @NotNull
    @Contract(pure = true)
    private String buildReqExample(String reqExampleType, String reqExample) {

        return "```" + reqExampleType + "\n" +
                (reqExample == null ? "" : reqExample) + "\n" +
                "```";
    }

    @NotNull
    @Contract(pure = true)
    private String buildRespExample(String respExampleType, String respExample) {
        return "```json\n" +
                (respExampleType == null ? "" : respExample) + "\n" +
                "```\n\n";
    }

}

package com.yupi.springbootinit.manager;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the two pieces returned by the chart-analysis model.
 *
 * <p>The original project only accepted the literal
 * {@code 【【【【【} separator. Different models, and even different
 * responses from the same model, may instead return a JSON envelope,
 * Markdown code fences, or labelled sections. Keeping the compatibility
 * parsing here prevents the controller and asynchronous consumers from
 * having subtly different behaviour.</p>
 */
public final class AiResponseParser {

    private static final String SEPARATOR = "【【【【【";
    private static final Pattern CODE_BLOCK = Pattern.compile(
            "(?is)```(?:json|javascript|js|echarts)?\\s*(.*?)```");
    private static final Pattern LABELLED_SECTIONS = Pattern.compile(
            "(?is)(?:图表配置|ECharts\\s*(?:配置|option)?|chart\\s*option|option)"
                    + "\\s*[:：]?\\s*(.*?)\\s*"
                    + "(?:数据分析结论|分析结论|分析结果|结论)\\s*[:：]?\\s*(.*)$");

    private AiResponseParser() {
    }

    /**
     * Parses a model answer into the chart option JSON and the analysis text.
     *
     * @throws IllegalArgumentException when the answer contains no recognisable
     *                                  chart/conclusion pair
     */
    public static ParsedResult parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("AI 返回内容为空");
        }
        String content = raw.replace("\r\n", "\n").trim();

        ParsedResult parsed = parseSeparator(content);
        if (parsed != null) {
            return parsed;
        }

        parsed = parseJsonEnvelope(content);
        if (parsed != null) {
            return parsed;
        }

        parsed = parseTaggedSections(content);
        if (parsed != null) {
            return parsed;
        }

        parsed = parseCodeFence(content);
        if (parsed != null) {
            return parsed;
        }

        parsed = parseLabelledSections(content);
        if (parsed != null) {
            return parsed;
        }

        throw new IllegalArgumentException("AI 返回内容缺少图表配置和分析结论");
    }

    private static ParsedResult parseSeparator(String content) {
        String[] sections = content.split(Pattern.quote(SEPARATOR), -1);
        if (sections.length < 3) {
            return null;
        }
        String chart = cleanChart(sections[1]);
        String conclusion = joinFrom(sections, 2, SEPARATOR).trim();
        return valid(chart, conclusion) ? new ParsedResult(chart, conclusion) : null;
    }

    private static ParsedResult parseJsonEnvelope(String content) {
        String candidate = stripSingleCodeFence(content).trim();
        if (!candidate.startsWith("{")) {
            int firstBrace = candidate.indexOf('{');
            int lastBrace = candidate.lastIndexOf('}');
            if (firstBrace < 0 || lastBrace <= firstBrace) {
                return null;
            }
            candidate = candidate.substring(firstBrace, lastBrace + 1);
        }
        try {
            JSONObject object = JSONUtil.parseObj(candidate);
            Object chartValue = firstValue(object,
                    "genChart", "chart", "option", "chartOption", "图表配置");
            Object conclusionValue = firstValue(object,
                    "genResult", "analysis", "conclusion", "result", "分析结论", "结论");
            if (chartValue == null || conclusionValue == null) {
                return null;
            }
            String chart = cleanChart(asText(chartValue));
            String conclusion = asText(conclusionValue).trim();
            return valid(chart, conclusion) ? new ParsedResult(chart, conclusion) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ParsedResult parseTaggedSections(String content) {
        String chart = betweenIgnoreCase(content,
                "<chart>", "</chart>", "<echarts>", "</echarts>", "<option>", "</option>");
        String conclusion = betweenIgnoreCase(content,
                "<conclusion>", "</conclusion>", "<analysis>", "</analysis>",
                "<genresult>", "</genresult>");
        if (chart == null || conclusion == null) {
            return null;
        }
        chart = cleanChart(chart);
        conclusion = conclusion.trim();
        return valid(chart, conclusion) ? new ParsedResult(chart, conclusion) : null;
    }

    private static ParsedResult parseCodeFence(String content) {
        Matcher matcher = CODE_BLOCK.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        String chart = cleanChart(matcher.group(1));
        int firstBlockEnd = matcher.end();
        String conclusion;
        if (matcher.find()) {
            conclusion = matcher.group(1).trim();
        } else {
            conclusion = content.substring(firstBlockEnd).trim();
        }
        conclusion = removeConclusionLabel(conclusion);
        return valid(chart, conclusion) ? new ParsedResult(chart, conclusion) : null;
    }

    private static ParsedResult parseLabelledSections(String content) {
        Matcher matcher = LABELLED_SECTIONS.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        String chart = cleanChart(matcher.group(1));
        String conclusion = removeConclusionLabel(matcher.group(2)).trim();
        return valid(chart, conclusion) ? new ParsedResult(chart, conclusion) : null;
    }

    private static Object firstValue(JSONObject object, String... keys) {
        for (String key : keys) {
            Object value = object.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String asText(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        return JSONUtil.toJsonStr(value);
    }

    private static String cleanChart(String chart) {
        if (chart == null) {
            return "";
        }
        String cleaned = stripSingleCodeFence(chart).trim();
        if (cleaned.startsWith("const option") || cleaned.startsWith("let option")
                || cleaned.startsWith("var option")) {
            int equals = cleaned.indexOf('=');
            if (equals >= 0) {
                cleaned = cleaned.substring(equals + 1).trim();
            }
        }
        // A model may prepend a short sentence before an otherwise valid JSON
        // object. Keep only the object when it is valid JSON.
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            String candidate = cleaned.substring(firstBrace, lastBrace + 1).trim();
            try {
                return JSONUtil.toJsonStr(JSONUtil.parseObj(candidate));
            } catch (Exception ignored) {
                // Keep the original option text; the caller can report a useful
                // format error instead of silently corrupting the model output.
            }
        }
        return cleaned;
    }

    private static String stripSingleCodeFence(String value) {
        Matcher matcher = CODE_BLOCK.matcher(value.trim());
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return value;
    }

    private static String removeConclusionLabel(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceFirst("(?is)^\\s*(?:数据分析结论|分析结论|分析结果|结论)\\s*[:：]?\\s*", "");
    }

    private static String betweenIgnoreCase(String content, String startTag,
                                            String endTag, String... alternatives) {
        String result = between(content, startTag, endTag);
        if (result != null) {
            return result;
        }
        for (int i = 0; i + 1 < alternatives.length; i += 2) {
            result = between(content, alternatives[i], alternatives[i + 1]);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static String between(String content, String startTag, String endTag) {
        String lower = content.toLowerCase();
        String start = startTag.toLowerCase();
        String end = endTag.toLowerCase();
        int startIndex = lower.indexOf(start);
        if (startIndex < 0) {
            return null;
        }
        startIndex += start.length();
        int endIndex = lower.indexOf(end, startIndex);
        if (endIndex < 0) {
            return null;
        }
        return content.substring(startIndex, endIndex);
    }

    private static String joinFrom(String[] values, int start, String separator) {
        StringBuilder result = new StringBuilder();
        for (int i = start; i < values.length; i++) {
            if (result.length() > 0) {
                result.append(separator);
            }
            result.append(values[i]);
        }
        return result.toString();
    }

    private static boolean valid(String chart, String conclusion) {
        return chart != null && !chart.isBlank() && conclusion != null && !conclusion.isBlank();
    }

    public static final class ParsedResult {
        private final String genChart;
        private final String genResult;

        private ParsedResult(String genChart, String genResult) {
            this.genChart = genChart;
            this.genResult = genResult;
        }

        public String getGenChart() {
            return genChart;
        }

        public String getGenResult() {
            return genResult;
        }
    }
}

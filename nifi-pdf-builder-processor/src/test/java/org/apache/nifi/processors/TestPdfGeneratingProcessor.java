package org.apache.nifi.processors;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class TestPdfGeneratingProcessor {
    @Test
    public void testValidSchema() {
        TestRunner runner = TestRunners.newTestRunner(PdfGeneratingProcessor.class);
        runner.setProperty(PdfGeneratingProcessor.TEMPLATE, "<p>Test</p>");
        runner.assertValid();
    }

    @Test
    public void testInvalidSchema() {
        TestRunner runner = TestRunners.newTestRunner(PdfGeneratingProcessor.class);
        runner.setProperty(PdfGeneratingProcessor.TEMPLATE, "<p>Test {{x}]</p>");
        runner.assertNotValid();
    }

    @Test
    public void testConvertProperties() throws Exception {
        MockFlowFile mff = new MockFlowFile(System.currentTimeMillis());
        mff.putAttributes(new HashMap<String, String>() {{
            put("schema.name", "test");
            put("mime.type", "application/json");
        }});

        Map<String, Object> json = PdfGeneratingProcessor.getMapFromAttributes(mff);
        Assert.assertNotNull(json);
        Assert.assertNotNull(json.get("schema"));
        Assert.assertEquals(((Map)json.get("schema")).get("name"), "test");
        Assert.assertNotNull(json.get("mime"));
        Assert.assertEquals(((Map)json.get("mime")).get("type"), "application/json");
    }

    @Test
    public void testVerySimpleTemplate() {
        TestRunner runner = TestRunners.newTestRunner(PdfGeneratingProcessor.class);
        runner.setProperty(PdfGeneratingProcessor.TEMPLATE, "<p>Test</p>");
        runner.setProperty(PdfGeneratingProcessor.TEMPLATE_CONTEXT, PdfGeneratingProcessor.SOURCE_ATTRIBUTE);
        runner.enqueue("");
        runner.run();

        dumpToTemp(runner);
    }

    @Test
    public void testFromAttributes() throws Exception {
        byte[] tmpl = IOUtils.toByteArray(new FileInputStream("src/test/resources/attribute_template.tpl"));

        TestRunner runner = TestRunners.newTestRunner(PdfGeneratingProcessor.class);
        runner.setProperty(PdfGeneratingProcessor.TEMPLATE, new String(tmpl));
        runner.setProperty(PdfGeneratingProcessor.TEMPLATE_CONTEXT, PdfGeneratingProcessor.SOURCE_ATTRIBUTE);
        runner.enqueue("", new HashMap<String, String>() {{
            put("schema.name", "test_schema");
            put("mime.type", "application/json");
        }});
        runner.run();

        testCounts(runner);
    }

    @Test
    public void testFromBoth() throws Exception {
        byte[] json = IOUtils.toByteArray(new FileInputStream("src/test/resources/report_template.json"));
        byte[] tmpl = IOUtils.toByteArray(new FileInputStream("src/test/resources/both_template.tpl"));

        TestRunner runner = TestRunners.newTestRunner(PdfGeneratingProcessor.class);
        runner.setProperty(PdfGeneratingProcessor.TEMPLATE, new String(tmpl));
        runner.setProperty(PdfGeneratingProcessor.TEMPLATE_CONTEXT, PdfGeneratingProcessor.SOURCE_BOTH);
        runner.enqueue(json, new HashMap<String, String>() {{
            put("schema.name", "test_schema");
            put("mime.type", "application/json");
        }});
        runner.run();

        testCounts(runner);
    }

    @Test
    public void testFromBody() throws Exception {
        byte[] json = IOUtils.toByteArray(new FileInputStream("src/test/resources/report_template.json"));
        byte[] tmpl = IOUtils.toByteArray(new FileInputStream("src/test/resources/report_template.tpl"));

        TestRunner runner = TestRunners.newTestRunner(PdfGeneratingProcessor.class);
        runner.setProperty(PdfGeneratingProcessor.TEMPLATE, new String(tmpl));
        runner.setProperty(PdfGeneratingProcessor.TEMPLATE_CONTEXT, PdfGeneratingProcessor.SOURCE_BODY);
        runner.enqueue(json);
        runner.run();

        testCounts(runner);
    }

    @Test
    public void testBodyFromArray() throws Exception {
        byte[] json = IOUtils.toByteArray(new FileInputStream("src/test/resources/list_report.json"));
        byte[] tmpl = IOUtils.toByteArray(new FileInputStream("src/test/resources/list_report.tpl"));

        TestRunner runner = TestRunners.newTestRunner(PdfGeneratingProcessor.class);
        runner.setProperty(PdfGeneratingProcessor.TEMPLATE, new String(tmpl));
        runner.setProperty(PdfGeneratingProcessor.TEMPLATE_CONTEXT, PdfGeneratingProcessor.SOURCE_BODY);
        runner.enqueue(json);
        runner.run();

        testCounts(runner);
    }

    @Test
    public void testFromComplexBody() throws Exception {
        byte[] json = IOUtils.toByteArray(new FileInputStream("src/test/resources/blog_index.json"));
        byte[] tmpl = IOUtils.toByteArray(new FileInputStream("src/test/resources/index.tpl"));

        TestRunner runner = TestRunners.newTestRunner(PdfGeneratingProcessor.class);
        runner.setProperty(PdfGeneratingProcessor.TEMPLATE, new String(tmpl));
        runner.setProperty(PdfGeneratingProcessor.TEMPLATE_CONTEXT, PdfGeneratingProcessor.SOURCE_BODY);
        runner.enqueue(json);
        runner.run();

        testCounts(runner);

        dumpToTemp(runner);
    }

    private void testCounts(TestRunner runner) {
        runner.assertTransferCount(PdfGeneratingProcessor.REL_SUCCESS, 1);
        runner.assertTransferCount(PdfGeneratingProcessor.REL_ORIGINAL, 1);
        runner.assertTransferCount(PdfGeneratingProcessor.REL_FAILURE, 0);
    }

    private void dumpToTemp(TestRunner runner) {
        MockFlowFile mff = runner.getFlowFilesForRelationship(PdfGeneratingProcessor.REL_SUCCESS).get(0);
        byte[] content = runner.getContentAsByteArray(mff);
        try {
            File path = new File(String.format("/tmp/%s.pdf", mff.getAttribute("uuid")));
            FileOutputStream writer = new FileOutputStream(path);
            writer.write(content);
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

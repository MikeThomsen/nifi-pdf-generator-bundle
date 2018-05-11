package org.apache.nifi.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.itextpdf.html2pdf.HtmlConverter;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Tags({ "pdf", "generator", "html" })
@CapabilityDescription("Provides the ability to build a PDF file from a HTML file that is generated from a user supplied template.")
public class PdfGeneratingProcessor extends AbstractProcessor {
    static final PropertyDescriptor TEMPLATE = new PropertyDescriptor.Builder()
        .name("pdf-generator-template")
        .displayName("Template")
        .description("A template body written in the Mustache template language.")
        .required(true)
        .addValidator((subject, input, validationContext) -> {
            if (StringUtils.isBlank(input)) {
                return new ValidationResult.Builder()
                    .explanation("Blank templates are not allowed.")
                    .input(input)
                    .subject(subject)
                    .valid(false)
                    .build();
            }

            try {
                new DefaultMustacheFactory().compile(new StringReader(input), "validation-test");
                return new ValidationResult.Builder()
                    .valid(true)
                    .build();
            } catch (Exception ex) {
                ex.printStackTrace();
                return new ValidationResult.Builder()
                    .explanation(ex.getMessage())
                    .input(input)
                    .subject(subject)
                    .valid(false)
                    .build();
            }
        })
        .expressionLanguageSupported(false)
        .build();

    static final AllowableValue SOURCE_ATTRIBUTE = new AllowableValue("attr", "Flowfile Attributes");
    static final AllowableValue SOURCE_BODY      = new AllowableValue("body", "Flowfile Body");
    static final AllowableValue SOURCE_BOTH      = new AllowableValue("both", "Both");

    static final PropertyDescriptor TEMPLATE_CONTEXT = new PropertyDescriptor.Builder()
        .name("pdf-generator-context")
        .displayName("Context Source")
        .description("The \"context\" is the data model used to power the template. It's where all of the variables " +
                "that are injected into the template come from. The source of the context can be an incoming JSON object " +
                "from a flowfile body, the flowfile attributes or both. If both is chosen, the context will scope attributes to " +
                "an attribute branch and flowfile json properties to a flowfile branch. See docs for additional details.")
        .addValidator(Validator.VALID)
        .allowableValues(SOURCE_ATTRIBUTE, SOURCE_BODY, SOURCE_BOTH)
        .expressionLanguageSupported(false)
        .defaultValue(SOURCE_ATTRIBUTE.getValue())
        .build();


    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        List _temp = new ArrayList();
        _temp.add(TEMPLATE);
        _temp.add(TEMPLATE_CONTEXT);

        return Collections.unmodifiableList(_temp);
    }

    static final Relationship REL_ORIGINAL = new Relationship.Builder()
        .name("original")
        .description("All input flowfiles go to this relationship.")
        .build();

    static final Relationship REL_SUCCESS = new Relationship.Builder()
        .name("success")
        .description("All successful output flowfiles go here.")
        .build();
    static final Relationship REL_FAILURE = new Relationship.Builder()
        .name("failure")
        .description("All failed input flowfiles go here.")
        .build();

    @Override
    public Set<Relationship> getRelationships() {
        Set _rels = new HashSet();
        _rels.add(REL_ORIGINAL);
        _rels.add(REL_SUCCESS);
        _rels.add(REL_FAILURE);

        return Collections.unmodifiableSet(_rels);
    }

    private Mustache compiledTemplate;

    @OnScheduled
    public void onScheduled(ProcessContext context) {
        final String template = context.getProperty(TEMPLATE).getValue();
        this.compiledTemplate = new DefaultMustacheFactory().compile(new StringReader(template), "pdf-template");
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        FlowFile input = session.get();
        if (input == null) {
            return;
        }

        Map<String, Object> templateContext;

        try {
            templateContext = buildContext(context, session, input);
        } catch (IOException e) {
            getLogger().error("Could not build context.", e);
            throw new ProcessException(e);
        }

        try {
            StringWriter writer = new StringWriter();
            compiledTemplate.execute(writer, templateContext);
            writer.close();

            final String result = writer.toString();

            if (getLogger().isDebugEnabled()) {
                getLogger().debug(String.format("Built this HTML from the template:\n\n%s", result));
            }

            FlowFile output = session.create(input);
            output = session.write(output, out -> {
                HtmlConverter.convertToPdf(result, out);
            });
            output = session.putAttribute(output, CoreAttributes.MIME_TYPE.key(), "application/pdf");

            session.transfer(input, REL_ORIGINAL);
            session.transfer(output, REL_SUCCESS);
            session.getProvenanceReporter().create(output);
        } catch (IOException e) {
            getLogger().error("Error building PDF: ", e);
            session.transfer(input, REL_FAILURE);
        }
    }

    public static Map<String, Object> buildContext(ProcessContext context, ProcessSession session, FlowFile flowFile) throws IOException {
        final String source = context.getProperty(TEMPLATE_CONTEXT).getValue();
        Map<String, Object> templateContext = new HashMap<>();
        Map<String, Object> fromAttributes = new HashMap<>();
        Map<String, Object> fromBody = new HashMap<>();

        if (source.equals(SOURCE_BOTH.getValue()) || source.equals(SOURCE_BODY.getValue())) {
            fromBody.putAll(getFromBody(session, flowFile));
        }

        if (source.equals(SOURCE_BOTH.getValue()) || source.equals(SOURCE_ATTRIBUTE.getValue())) {
            fromAttributes.putAll(getMapFromAttributes(flowFile));
        }

        if (fromBody.size() > 0 && fromAttributes.size() > 0) {
            templateContext.put("attributes", fromAttributes);
            templateContext.put("flowfile", fromBody);
        } else if (fromBody.size() > 0) {
            templateContext.putAll(fromBody);
        } else if (fromAttributes.size() > 0) {
            templateContext.putAll(fromAttributes);
        }

        return templateContext;
    }

    public static Map<String, Object> getFromBody(ProcessSession session, FlowFile input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        session.exportTo(input, out);
        out.close();

        String raw = new String(out.toByteArray());
        Map<String, Object> retVal = new HashMap<>();

        if (raw.startsWith("[") && raw.endsWith("]")) {
            retVal.put("content", new ObjectMapper().readValue(raw, List.class));
        } else {
            retVal.putAll(new ObjectMapper().readValue(raw, Map.class));
        }

        return retVal;
    }

    public static Map getMapFromAttributes(FlowFile input) throws IOException {
        Map<String, String> attrs = input.getAttributes();
        JavaPropsMapper mapper = new JavaPropsMapper();
        Properties props = new Properties();
        for (Map.Entry<String, String> attr : attrs.entrySet()) {
            props.setProperty(attr.getKey(), attr.getValue());
        }

        return mapper.readPropertiesAs(props, Map.class);
    }
}

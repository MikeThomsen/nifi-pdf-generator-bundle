## NiFi PDF Generator

This package provides a processor that can generate a PDF file from flowfiles. It uses Mustache templates to generate HTML, which is in turn converted to PDF using the iText html2pdf package. The Mustache templates are powered using either flowfile body content in the form of a well-formed JSON document, the flowfile attributes or a combination of the two.

## NiFi PDF Generator

This package provides a processor that can generate a PDF file from flowfiles. It uses Mustache templates to 
generate HTML, which is in turn converted to PDF using the iText html2pdf package. The Mustache templates are 
powered using either flowfile body content in the form of a well-formed JSON document, the flowfile 
attributes or a combination of the two.

Requirements:

* NiFi 1.6.0 or 1.7.0-SNAPSHOT
    * Only tested under 1.7.0-SNAPSHOT
* JSON input and/or flowfile with user-supplied attributes.

Configuration Properties:

* Template: a [Mustatche template](https://github.com/spullara/mustache.java) string.
* Template Context: the location of the variables that will be supplied to the template.
    * FlowFile Attributes - only attributes on the flowfile will be exposed to the template.
    * FlowFile Body - a well-formed JSON string in the flowfile content/body.
        * If the JSON is an array, it will be converted to match this structure: `{ "content": [] }`
    * Both - attributes and content will be used.
        * Attributes will be added to an `attributes` branch.
        * Content from the body will be added to a `flowfile` branch.


### Example of "Both"

```json
{
  "attributes": {
    "schema": {
      "name": "my_test_record"
    },
    "mime": {
      "type": "application/json"
    },
    "filename": "test.json",
    "uuid": "UUID_HERE"
  },
  "flowfile": {
    "departments": [
      {
        "name": "Engineering",
        "employees": [
          "John Smith",
          "Jane Doe"
        ]
      }
    ]
  }
}
```
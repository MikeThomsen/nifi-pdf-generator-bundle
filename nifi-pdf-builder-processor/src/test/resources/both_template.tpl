<h1>FlowFile Attributes:</h1>

<ul>
    <li>Mime Type: {{attributes.mime.type}}</li>
    <li>Schema Name: {{attributes.schema.name}}</li>
    <li>File Name: {{attributes.filename}}</li>
    <li>Path: {{attributes.path}}</li>
    <li>UUID: {{attributes.uuid}}</li>
</ul>

<p>Department: {{flowfile.department}}</p>

<ul>
{{#flowfile.users}}
    <li>{{name}} - bad logins: {{bad_logins}}</li>
{{/flowfile.users}}
</ul>
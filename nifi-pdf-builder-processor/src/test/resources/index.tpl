<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <title>{{title}}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <script src="/js/jquery.min.js"></script>
    <script src="/js/bootstrap.min.js"></script>
    <link href="src/test/resources/css/bootstrap.min.css" rel="stylesheet" type="text/css">
    <link href="src/test/resources/css/theme.css" rel="stylesheet" type="text/css">
    <link href="src/test/resources/css/syntax.css" rel="stylesheet" type="text/css">


</head>

<body>

<div class="container-fluid">
    <div class="row-fluid">
        <div class="navbar navbar-inverse navbar-fixed-top" role="navigation">
            <div class="navbar-header">
                <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
                  <span class="sr-only">Toggle navigation</span>
                  <span class="icon-bar"></span>
                  <span class="icon-bar"></span>
                  <span class="icon-bar"></span>
                </button>
                <a class="navbar-brand" href="/">{{title}}</a>
              </div>
              <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
                <ul class="nav navbar-nav">
                    <li class="active"><a href="/">Home</a></li>
                    <li class="active visible-xs-block"><a href="/links.html">Links</a></li>
                    <li class="active"><a href="/archive.html">Archive</a></li>
                    <li class="active"><a href="/about.html">About</a></li>
                    <li class="active"><a href="/feed.xml">RSS</a></li>
                    
                    
                      <li class="active"><a href="{{url}}">Github</a></li>
                    
                </ul>
              </div>
        </div>
    </div>
</div>


<div class="container container-left">
    <div class="row">
        <div class="col-md-3 hidden-xs">
            <div class="sidebar well">
Random commentary about software development
</div>

<div class="sidebar well">
    <h1>Recent Posts</h1>

    <ul>
    {{#recent}}
          <li><a href="{{url}}">{{title}}</a></li>
    {{/recent}}
    </ul>
</div>

<div class="sidebar well">
<h1>Links</h1>
<ul>
    {{#sidebar_links}}
    <li><a href="{{url}}">{{title}}</a></li>
    {{/sidebar_links}}
</ul>

</div>

        </div>
        <div class="col-md-9">

{{#articles}}
<div class="article">
    <div class="well">
        <h1><a href="{{url}}">May 7, 2018 - {{title}}</a></h1>
        
        <div class="content">
            {{#content}}
            <p>{{.}}</p>
            {{/content}}
        </div>
    </div>
</div>
{{/articles}}

<div class="pagination">
  
  <span class="page_number ">Page: 1 of 1</span>
  
</div>

        </div>
    </div>
</div>

<div class="container-fluid">
    <div class="row-fluid">
        <div class="span12 footer navbar-inverse navbar-fixed-bottom">
            <p class="copyright">&copy;2018 {{title}}. Powered by <a href="http://jekyllrb.com">Jekyll</a>, theme by <a href="https://github.com/scotte/jekyll-clean">Scott Emmons</a>
            under
            <a href="http://creativecommons.org/licenses/by/4.0/">Creative Commons Attribution</a></p>
        </div>
    </div>
</div>

</body>
</html>


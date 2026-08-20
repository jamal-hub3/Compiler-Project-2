parser grammar HtmlCssParser;

options {
    tokenVocab = HtmlCssLexer;
}

// ===========================
// نقطة البداية — الوثيقة الكاملة
// ===========================
htmlDocument
    : ( htmlElement
      | htmlChardata
      | jinjaExpression
      | jinjaStatement
      | DTD
      | XML
      )* EOF
    ;

// النص العادي بين الوسوم
htmlChardata
    : HTML_TEXT
    ;

// العنصر — إما عادي أو self-closing أو style
htmlElement
    : TAG_OPEN TAG_NAME htmlAttribute* TAG_SLASH_CLOSE                           # selfClosingElement
    | TAG_OPEN TAG_NAME htmlAttribute* TAG_CLOSE htmlContent
      TAG_OPEN TAG_SLASH TAG_NAME TAG_CLOSE                                      # normalElement
    | style                                                                      # styleElement
    ;

// محتوى داخل الوسوم
htmlContent
    : ( htmlChardata
      | htmlElement
      | CDATA
      | jinjaExpression
      | jinjaStatement
      )*
    ;

// الخاصية (attribute)
htmlAttribute
    : TAG_NAME (TAG_EQUALS ATTVALUE_VALUE)?
    ;

// كتلة الـ CSS
style
    : STYLE_OPEN (STYLE_BODY | STYLE_SHORT_BODY)
    ;

// تعبير Jinja: {{ ... }}
jinjaExpression
    : JINJA_EXPRESSION_START JINJA_EXPRESSION_CONTENT JINJA_EXPRESSION_END
    ;

// جملة Jinja: {% ... %}
jinjaStatement
    : JINJA_STATEMENT_START JINJA_STATEMENT_CONTENT JINJA_STATEMENT_END
    ;

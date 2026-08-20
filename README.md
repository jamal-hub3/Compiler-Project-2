# مشروع المترجمات 2025/2026 — Flask + Jinja2 + HTML + CSS + Python

يطبّق هذا المشروع آلية التوليد الموضحة بالضبط:

```
app.py → Python Parser → Python AST → Semantic Analysis → Generator
   → Context Data → render_template() → Jinja Parser → Jinja AST
   → تبديل المتغيرات {{ name }} → HTML → المتصفح
```

## هيكل المشروع

```
templates/                  ← ملفات الدخل (Input) بامتداد .jinja
├── base.jinja
├── index.jinja
├── add_product.jinja
└── edit_product.jinja

python_input/app.py         ← ملف الدخل Python (Flask backend)

flask_app/                  ← التطبيق الحي (يعمل عبر Flask الحقيقي)
├── app.py
├── templates/*.html
└── static/{style.css, script.js}

src/                        ← الكود المصدري للمترجم (Java)
├── Main.java
├── Rules/                  ← ANTLR grammar (HTML/CSS/Jinja مدمجة)
├── AST/python/              ← الشجرة الأولى (Python AST)
├── AST/jinja2/               ← الشجرة الثانية (Jinja2 AST)
├── semantic/                 ← محللان دلاليان (Python + Jinja2، 5 أخطاء لكل جزء)
└── generator/
    ├── Jinja2Generator.java  ← يبدّل المتغيرات وينتج HTML
    ├── LayoutMerger.java     ← يحلّ {% extends %} فعلياً (يدمج base.jinja)
    └── PythonGenerator.java
```

## المخرجات (تُنشأ تلقائياً عند تشغيل Main.java)

```
output/                     ← الخرج الحقيقي من المترجم
├── index.html              ← مُولَّد من templates/index.jinja
├── add_product.html        ← مُولَّد
├── edit_product.html       ← مُولَّد
├── app.py                  ← منسوخ كما هو (بدون معالجة)
├── style.css                ← منسوخ كما هو
└── script.js                 ← منسوخ كما هو

compiler_output/            ← نتائج مراحل التحليل والتوليد
├── ast_python.json
├── ast_jinja.json
├── semantic_report.txt
└── generation_log.txt
```

## التشغيل

**المترجم (Java):** شغّل `Main.java` في IntelliJ (Working Directory = مجلد المشروع الرئيسي).

**التطبيق الحي (Flask):**
```bash
cd flask_app
pip install flask
python app.py
```

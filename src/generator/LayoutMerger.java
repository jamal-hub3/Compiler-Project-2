package generator;

import AST.jinja2.*;

import java.util.*;

/**
 * يحلّ {% extends "base.html" %} فعلياً — بدل طباعتها كتعليق فقط.
 *
 * يأخذ شجرة القالب الأب (base.html) وشجرة القالب الابن (index.html مثلاً)،
 * يستخرج كتل الـ {% block %} المُعرَّفة في الابن، ويحقنها داخل نفس الكتل
 * بالاسم نفسه في شجرة الأب — تماماً كما تعمل آلية الوراثة في Jinja2 الحقيقية.
 *
 * النتيجة: شجرة أب مكتملة (بكل الـ <head>, <nav>, <footer> الخاصة بها)
 * لكن بمحتوى الـ block المُستبدَل من الابن — جاهزة للتوليد كصفحة HTML كاملة.
 */
public class LayoutMerger {

    /**
     * @param base  شجرة القالب الأب كما بُنيت طازجة (يُفضَّل شجرة جديدة لكل استدعاء
     *              لتفادي تلوّث نفس الشجرة عبر أكثر من قالب ابن)
     * @param child شجرة القالب الابن (index.html، add_product.html، ...)
     * @return شجرة الأب نفسها بعد حقن محتوى الابن داخل الـ blocks المطابقة بالاسم
     */
    public static Jinja2TemplateNode merge(Jinja2TemplateNode base, Jinja2TemplateNode child) {
        Map<String, List<Jinja2Node>> childBlocks = extractTopLevelBlocks(child);
        injectBlocks(base, childBlocks);
        return base;
    }

    /** هل هذه الشجرة تحتوي {% extends %} في مستواها الأعلى؟ إن وُجدت أرجع اسم الأب */
    public static String findExtendsTarget(Jinja2TemplateNode template) {
        for (Jinja2Node child : template.getChildren()) {
            if (child instanceof Jinja2ExtendsNode ext) return ext.getParentTemplate();
        }
        return null;
    }

    // ── استخراج بلوكات الابن (باسمها) ───────────────────────────────

    private static Map<String, List<Jinja2Node>> extractTopLevelBlocks(Jinja2TemplateNode child) {
        Map<String, List<Jinja2Node>> map = new LinkedHashMap<>();
        for (Jinja2Node c : child.getChildren()) {
            if (c instanceof Jinja2BlockNode b) {
                map.put(b.getBlockName(), new ArrayList<>(b.getBody()));
            }
        }
        return map;
    }

    // ── حقن البلوكات داخل شجرة الأب (بحث متكرر عبر كل الأعماق) ──────

    private static void injectBlocks(Jinja2Node node, Map<String, List<Jinja2Node>> overrides) {
        if (node instanceof Jinja2TemplateNode t) {
            for (Jinja2Node c : t.getChildren()) injectBlocks(c, overrides);
        } else if (node instanceof Jinja2BlockNode b) {
            if (overrides.containsKey(b.getBlockName())) {
                b.setBody(overrides.get(b.getBlockName()));   // استبدل بمحتوى الابن
            } else {
                for (Jinja2Node c : b.getBody()) injectBlocks(c, overrides); // ابحث بالأعماق
            }
        } else if (node instanceof Jinja2HtmlElementNode e) {
            for (Jinja2Node c : e.getChildren()) injectBlocks(c, overrides);
        } else if (node instanceof Jinja2ForNode f) {
            for (Jinja2Node c : f.getBody()) injectBlocks(c, overrides);
        } else if (node instanceof Jinja2IfNode i) {
            for (Jinja2Node c : i.getThenBody()) injectBlocks(c, overrides);
            for (Jinja2Node c : i.getElseBody()) injectBlocks(c, overrides);
        }
        // Jinja2RawTextNode, Jinja2VariableNode, Jinja2ExtendsNode, Jinja2MacroNode → أوراق، لا شيء لفعله
    }
}

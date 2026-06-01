package me.tuanzi.client.update;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ARGB;

public class SimpleMarkdownParser {

    /**
     * 将 Markdown 语法的文本解析为 Minecraft 的带有富文本样式的 Component
     */
    public static Component parse(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return Component.empty();
        }

        String[] lines = markdown.split("\\r?\\n");
        MutableComponent root = Component.empty();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            MutableComponent lineComponent;

            // 1. 解析标题 (Headings)
            if (line.startsWith("### ")) {
                String content = line.substring(4);
                lineComponent = parseLineInlineStyles(content)
                        .withStyle(style -> style.withBold(true).withColor(ARGB.color(255, 100, 200, 255))); // 优雅淡蓝
            } else if (line.startsWith("## ")) {
                String content = line.substring(3);
                lineComponent = parseLineInlineStyles(content)
                        .withStyle(style -> style.withBold(true).withColor(ARGB.color(255, 255, 215, 0))); // 金黄
            } else if (line.startsWith("# ")) {
                String content = line.substring(2);
                lineComponent = parseLineInlineStyles(content)
                        .withStyle(style -> style.withBold(true).withColor(ARGB.color(255, 255, 100, 100))); // 浅红
            } 
            // 2. 解析列表 (Unordered Lists)
            else if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ")) {
                String content = line.substring(2);
                lineComponent = Component.literal("  • ")
                        .withStyle(style -> style.withColor(ARGB.color(255, 200, 200, 200)));
                lineComponent.append(parseLineInlineStyles(content));
            } 
            // 3. 普通行
            else {
                lineComponent = parseLineInlineStyles(line);
            }

            root.append(lineComponent);

            // 如果不是最后一行，追加换行符
            if (i < lines.length - 1) {
                root.append(Component.literal("\n"));
            }
        }

        return root;
    }

    /**
     * 解析单行内的加粗 (**)、斜体 (*)、以及代码块 (`) 等行内样式
     */
    private static MutableComponent parseLineInlineStyles(String text) {
        MutableComponent lineComp = Component.empty();
        int i = 0;
        int len = text.length();

        StringBuilder currentSegment = new StringBuilder();
        boolean isBold = false;
        boolean isItalic = false;
        boolean isCode = false;

        while (i < len) {
            // 检查加粗 **
            if (i + 1 < len && text.charAt(i) == '*' && text.charAt(i + 1) == '*') {
                if (currentSegment.length() > 0) {
                    lineComp.append(createStyledComponent(currentSegment.toString(), isBold, isItalic, isCode));
                    currentSegment.setLength(0);
                }
                isBold = !isBold;
                i += 2;
            } 
            // 检查斜体 *
            else if (text.charAt(i) == '*') {
                if (currentSegment.length() > 0) {
                    lineComp.append(createStyledComponent(currentSegment.toString(), isBold, isItalic, isCode));
                    currentSegment.setLength(0);
                }
                isItalic = !isItalic;
                i++;
            } 
            // 检查代码块 `
            else if (text.charAt(i) == '`') {
                if (currentSegment.length() > 0) {
                    lineComp.append(createStyledComponent(currentSegment.toString(), isBold, isItalic, isCode));
                    currentSegment.setLength(0);
                }
                isCode = !isCode;
                i++;
            } 
            // 普通字符
            else {
                currentSegment.append(text.charAt(i));
                i++;
            }
        }

        if (currentSegment.length() > 0) {
            lineComp.append(createStyledComponent(currentSegment.toString(), isBold, isItalic, isCode));
        }

        return lineComp;
    }

    private static MutableComponent createStyledComponent(String text, boolean bold, boolean italic, boolean code) {
        MutableComponent comp = Component.literal(text);
        if (bold) {
            comp.withStyle(style -> style.withBold(true));
        }
        if (italic) {
            comp.withStyle(style -> style.withItalic(true));
        }
        if (code) {
            // 用亮橙黄且加粗倾斜的样式表示代码块，非常有高级工业质感
            comp.withStyle(style -> style.withColor(ARGB.color(255, 255, 165, 0)).withBold(true).withItalic(true));
        }
        return comp;
    }
}

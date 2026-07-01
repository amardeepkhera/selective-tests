(() => {
    const holder = document.getElementById("testInstructionsEditor");
    const textarea = document.getElementById("testInstructions");
    const form = textarea?.closest("form");
    const initialHtml = holder?.dataset.initialHtml ?? "";
    const listTool = window.EditorjsList || window.List;

    if (!holder || !textarea || !form) {
        return;
    }

    if (typeof EditorJS === "undefined") {
        textarea.classList.remove("visually-hidden");
        textarea.classList.add("form-control");
        textarea.setAttribute("rows", "6");
        return;
    }

    const buildInitialData = (html) => {
        const trimmed = (html || "").trim();
        if (!trimmed) {
            return undefined;
        }

        return {
            blocks: [
                {
                    type: "paragraph",
                    data: {
                        text: trimmed,
                    },
                },
            ],
        };
    };

    const escapeHtml = (value) => value
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");

    const renderListItems = (items) => items
        .map((item) => {
            if (typeof item === "string") {
                return `<li>${item}</li>`;
            }

            if (item && typeof item === "object") {
                const content = item.content ?? "";
                const nestedItems = Array.isArray(item.items) && item.items.length > 0
                    ? renderList(item.meta?.style ?? "unordered", item.items)
                    : "";
                return `<li>${content}${nestedItems}</li>`;
            }

            return "";
        })
        .join("");

    const renderList = (style, items) => {
        const tag = style === "ordered" ? "ol" : "ul";
        return `<${tag}>${renderListItems(items)}</${tag}>`;
    };

    const renderBlock = (block) => {
        if (block.type === "paragraph") {
            return `<p>${block.data?.text ?? ""}</p>`;
        }

        if (block.type === "header") {
            const level = Number(block.data?.level) || 2;
            const normalizedLevel = Math.min(Math.max(level, 1), 6);
            return `<h${normalizedLevel}>${block.data?.text ?? ""}</h${normalizedLevel}>`;
        }

        if (block.type === "list") {
            const items = Array.isArray(block.data?.items) ? block.data.items : [];
            return renderList(block.data?.style, items);
        }

        return `<p>${escapeHtml(JSON.stringify(block.data ?? {}))}</p>`;
    };

    const editorConfig = {
        holder: "testInstructionsEditor",
        minHeight: 180,
        placeholder: "Write the test instructions here",
        inlineToolbar: ["bold", "italic", "link"],
        data: buildInitialData(initialHtml),
    };

    if (listTool) {
        editorConfig.tools = {
            list: {
                class: listTool,
                inlineToolbar: true,
                config: {
                    defaultStyle: "unordered",
                },
            },
        };
    }

    const editor = new EditorJS(editorConfig);

    const syncEditorValue = async () => {
        const output = await editor.save();
        textarea.value = output.blocks.map(renderBlock).join("").trim();
    };

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        try {
            await syncEditorValue();
        } catch (error) {
            console.error("Editor.js save failed", error);
            return;
        }

        form.submit();
    });
})();

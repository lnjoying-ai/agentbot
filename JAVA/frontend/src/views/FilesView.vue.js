/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, ref } from "vue";
import { useI18n } from "../i18n";
import { getApiBaseUrl } from "../store/config";
const { t } = useI18n();
const files = ref([]);
const loading = ref(false);
const error = ref(null);
const lastUpdated = ref(null);
const currentPath = ref("");
const selectedPaths = ref(new Set());
const baseUrl = () => getApiBaseUrl() || window.location.origin;
const fileEntries = computed(() => files.value.filter(item => item.type === "file"));
const selectedCount = computed(() => selectedPaths.value.size);
const allSelected = computed(() => fileEntries.value.length > 0 && selectedPaths.value.size === fileEntries.value.length);
const pathSegments = computed(() => currentPath.value ? currentPath.value.split("/").filter(Boolean) : []);
const fetchFiles = async () => {
    loading.value = true;
    error.value = null;
    try {
        const query = currentPath.value ? `?path=${encodeURIComponent(currentPath.value)}` : "";
        const res = await fetch(`${baseUrl()}/api/workspace/files${query}`);
        if (!res.ok)
            throw new Error(await res.text());
        files.value = await res.json();
        selectedPaths.value = new Set();
        lastUpdated.value = new Date().toLocaleTimeString();
    }
    catch (err) {
        error.value = t("files.loadFailed");
    }
    finally {
        loading.value = false;
    }
};
const deleteFile = async (entry) => {
    const confirmed = confirm(t("files.deleteConfirm", { name: entry.name }));
    if (!confirmed)
        return;
    try {
        const res = await fetch(`${baseUrl()}/api/workspace/files?path=${encodeURIComponent(entry.path)}`, {
            method: "DELETE"
        });
        if (!res.ok)
            throw new Error(await res.text());
        files.value = files.value.filter(item => item.path !== entry.path);
        if (selectedPaths.value.has(entry.path)) {
            const next = new Set(selectedPaths.value);
            next.delete(entry.path);
            selectedPaths.value = next;
        }
    }
    catch (err) {
        error.value = t("files.deleteFailed");
    }
};
const downloadFile = async (entry) => {
    if (entry.type !== "file")
        return;
    try {
        const res = await fetch(`${baseUrl()}/api/workspace/files/download?path=${encodeURIComponent(entry.path)}`);
        if (!res.ok)
            throw new Error(await res.text());
        const blob = await res.blob();
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = entry.name;
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
    }
    catch (err) {
        error.value = t("files.downloadFailed");
    }
};
const toggleSelect = (entry) => {
    if (entry.type !== "file")
        return;
    const next = new Set(selectedPaths.value);
    if (next.has(entry.path)) {
        next.delete(entry.path);
    }
    else {
        next.add(entry.path);
    }
    selectedPaths.value = next;
};
const toggleSelectAll = () => {
    if (allSelected.value) {
        selectedPaths.value = new Set();
    }
    else {
        selectedPaths.value = new Set(fileEntries.value.map(file => file.path));
    }
};
const bulkDownload = async () => {
    if (selectedPaths.value.size === 0)
        return;
    for (const path of selectedPaths.value) {
        const file = files.value.find(item => item.path === path);
        if (file)
            await downloadFile(file);
    }
};
const bulkDelete = async () => {
    if (selectedPaths.value.size === 0)
        return;
    const confirmed = confirm(t("files.bulkDeleteConfirm", { count: selectedPaths.value.size }));
    if (!confirmed)
        return;
    try {
        const targets = Array.from(selectedPaths.value);
        for (const path of targets) {
            const res = await fetch(`${baseUrl()}/api/workspace/files?path=${encodeURIComponent(path)}`, {
                method: "DELETE"
            });
            if (!res.ok)
                throw new Error(await res.text());
        }
        files.value = files.value.filter(item => !selectedPaths.value.has(item.path));
        selectedPaths.value = new Set();
    }
    catch (err) {
        error.value = t("files.deleteFailed");
    }
};
const openDir = (entry) => {
    if (entry.type !== "dir")
        return;
    currentPath.value = entry.path;
    fetchFiles();
};
const navigateToRoot = () => {
    if (!currentPath.value)
        return;
    currentPath.value = "";
    fetchFiles();
};
const navigateToIndex = (index) => {
    const next = pathSegments.value.slice(0, index + 1).join("/");
    if (next === currentPath.value)
        return;
    currentPath.value = next;
    fetchFiles();
};
const formatSize = (size) => {
    if (!Number.isFinite(size))
        return "-";
    if (size < 1024)
        return `${size} B`;
    const kb = size / 1024;
    if (kb < 1024)
        return `${kb.toFixed(1)} KB`;
    const mb = kb / 1024;
    return `${mb.toFixed(1)} MB`;
};
const formatTime = (value) => {
    if (!value)
        return "-";
    const date = new Date(value);
    if (Number.isNaN(date.getTime()))
        return value;
    return date.toLocaleString();
};
onMounted(fetchFiles);
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['files-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['file-row']} */ ;
/** @type {__VLS_StyleScopedClasses['checkbox-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['checkbox-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['icon-button']} */ ;
/** @type {__VLS_StyleScopedClasses['icon-button']} */ ;
/** @type {__VLS_StyleScopedClasses['icon-button']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
(__VLS_ctx.t("files.title"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "files-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "files-summary" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.t("files.subtitle"));
if (__VLS_ctx.lastUpdated) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "muted" },
    });
    (__VLS_ctx.t("files.updatedAt", { time: __VLS_ctx.lastUpdated }));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "files-actions" },
});
if (__VLS_ctx.selectedCount) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "muted" },
    });
    (__VLS_ctx.t("files.selected", { count: __VLS_ctx.selectedCount }));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.bulkDownload) },
    ...{ class: "icon-button" },
    disabled: (__VLS_ctx.selectedCount === 0),
    title: (__VLS_ctx.t('files.bulkDownload')),
    'aria-label': (__VLS_ctx.t('files.bulkDownload')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    'stroke-width': "2",
    'stroke-linecap': "round",
    'stroke-linejoin': "round",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M7 10l5 5 5-5",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M12 15V3",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.bulkDelete) },
    ...{ class: "icon-button danger" },
    disabled: (__VLS_ctx.selectedCount === 0),
    title: (__VLS_ctx.t('files.bulkDelete')),
    'aria-label': (__VLS_ctx.t('files.bulkDelete')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    'stroke-width': "2",
    'stroke-linecap': "round",
    'stroke-linejoin': "round",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M3 6h18",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M8 6V4h8v2",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M6 6l1 14a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-14",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M10 11v6",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M14 11v6",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.fetchFiles) },
    ...{ class: "button secondary" },
    disabled: (__VLS_ctx.loading),
});
(__VLS_ctx.t("common.refresh"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "path-bar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "muted" },
});
(__VLS_ctx.t("files.path"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.navigateToRoot) },
    ...{ class: "path-segment" },
});
for (const [segment, index] of __VLS_getVForSourceType((__VLS_ctx.pathSegments))) {
    (segment + index);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "path-separator" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.navigateToIndex(index);
            } },
        ...{ class: "path-segment" },
    });
    (segment);
}
if (__VLS_ctx.loading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "muted" },
    });
    (__VLS_ctx.t("common.loading"));
}
else if (__VLS_ctx.files.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "muted" },
    });
    (__VLS_ctx.t("files.empty"));
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "file-table" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "file-row file-header" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "checkbox-cell" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ onChange: (__VLS_ctx.toggleSelectAll) },
        type: "checkbox",
        checked: (__VLS_ctx.allSelected),
        'aria-label': (__VLS_ctx.t('files.selectAll')),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    (__VLS_ctx.t("files.name"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    (__VLS_ctx.t("files.size"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    (__VLS_ctx.t("files.modified"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "actions" },
    });
    (__VLS_ctx.t("files.actions"));
    for (const [entry] of __VLS_getVForSourceType((__VLS_ctx.files))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "file-row" },
            key: (entry.path),
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "checkbox-cell" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
            ...{ onChange: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    if (!!(__VLS_ctx.files.length === 0))
                        return;
                    __VLS_ctx.toggleSelect(entry);
                } },
            type: "checkbox",
            checked: (__VLS_ctx.selectedPaths.has(entry.path)),
            disabled: (entry.type === 'dir'),
            'aria-label': (entry.name),
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "file-name" },
        });
        if (entry.type === 'dir') {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(__VLS_ctx.loading))
                            return;
                        if (!!(__VLS_ctx.files.length === 0))
                            return;
                        if (!(entry.type === 'dir'))
                            return;
                        __VLS_ctx.openDir(entry);
                    } },
                ...{ class: "dir-link" },
            });
            (entry.name);
        }
        else {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            (entry.name);
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
        (__VLS_ctx.formatSize(entry.size));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
        (__VLS_ctx.formatTime(entry.modifiedAt));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "actions" },
        });
        if (entry.type === 'file') {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(__VLS_ctx.loading))
                            return;
                        if (!!(__VLS_ctx.files.length === 0))
                            return;
                        if (!(entry.type === 'file'))
                            return;
                        __VLS_ctx.downloadFile(entry);
                    } },
                ...{ class: "icon-button" },
                title: (__VLS_ctx.t('files.download')),
                'aria-label': (__VLS_ctx.t('files.download')),
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
                viewBox: "0 0 24 24",
                fill: "none",
                stroke: "currentColor",
                'stroke-width': "2",
                'stroke-linecap': "round",
                'stroke-linejoin': "round",
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
                d: "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4",
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
                d: "M7 10l5 5 5-5",
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
                d: "M12 15V3",
            });
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    if (!!(__VLS_ctx.files.length === 0))
                        return;
                    __VLS_ctx.deleteFile(entry);
                } },
            ...{ class: "icon-button danger" },
            title: (__VLS_ctx.t('common.delete')),
            'aria-label': (__VLS_ctx.t('common.delete')),
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
            viewBox: "0 0 24 24",
            fill: "none",
            stroke: "currentColor",
            'stroke-width': "2",
            'stroke-linecap': "round",
            'stroke-linejoin': "round",
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
            d: "M3 6h18",
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
            d: "M8 6V4h8v2",
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
            d: "M6 6l1 14a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-14",
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
            d: "M10 11v6",
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
            d: "M14 11v6",
        });
    }
}
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "error-tip" },
    });
    (__VLS_ctx.error);
}
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['files-header']} */ ;
/** @type {__VLS_StyleScopedClasses['files-summary']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['files-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['icon-button']} */ ;
/** @type {__VLS_StyleScopedClasses['icon-button']} */ ;
/** @type {__VLS_StyleScopedClasses['danger']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['path-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['path-segment']} */ ;
/** @type {__VLS_StyleScopedClasses['path-separator']} */ ;
/** @type {__VLS_StyleScopedClasses['path-segment']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['file-table']} */ ;
/** @type {__VLS_StyleScopedClasses['file-row']} */ ;
/** @type {__VLS_StyleScopedClasses['file-header']} */ ;
/** @type {__VLS_StyleScopedClasses['checkbox-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['file-row']} */ ;
/** @type {__VLS_StyleScopedClasses['checkbox-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['file-name']} */ ;
/** @type {__VLS_StyleScopedClasses['dir-link']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['icon-button']} */ ;
/** @type {__VLS_StyleScopedClasses['icon-button']} */ ;
/** @type {__VLS_StyleScopedClasses['danger']} */ ;
/** @type {__VLS_StyleScopedClasses['error-tip']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            t: t,
            files: files,
            loading: loading,
            error: error,
            lastUpdated: lastUpdated,
            selectedPaths: selectedPaths,
            selectedCount: selectedCount,
            allSelected: allSelected,
            pathSegments: pathSegments,
            fetchFiles: fetchFiles,
            deleteFile: deleteFile,
            downloadFile: downloadFile,
            toggleSelect: toggleSelect,
            toggleSelectAll: toggleSelectAll,
            bulkDownload: bulkDownload,
            bulkDelete: bulkDelete,
            openDir: openDir,
            navigateToRoot: navigateToRoot,
            navigateToIndex: navigateToIndex,
            formatSize: formatSize,
            formatTime: formatTime,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */

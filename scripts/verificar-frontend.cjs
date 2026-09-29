const fs = require('node:fs');
const path = require('node:path');
const {execFileSync} = require('node:child_process');
const root=path.resolve(__dirname,'..');
let errors=[];
const pages=fs.readdirSync(root).filter(f=>f.endsWith('.html'));
for(const page of pages) {
    const html=fs.readFileSync(path.join(root,page),'utf8');
    const ids=[...html.matchAll(/\bid="([^"]+)"/g)].map(m=>m[1]);
    if(ids.length !== new Set(ids).size) errors.push(page+': IDs duplicados');
    for(const match of html.matchAll(/(?:src|href)="([^"]+)"/g)) {
        const link=match[1]; if(/^(?:https?:|#|data:|mailto:)/.test(link)) continue;
        if(!fs.existsSync(path.join(root,decodeURIComponent(link.split(/[?#]/)[0])))) errors.push(page+': caminho ausente '+link);
    }
    for(const match of html.matchAll(/<script src="(js\/[^\"]+)"/g)) {
        const file=match[1]; if(['js/api.js','js/ui.js','js/suporte.js'].includes(file)) continue;
        const js=fs.readFileSync(path.join(root,file),'utf8');
        for(const ref of js.matchAll(/getElementById\(['"]([^'"]+)['"]\)/g)) {
            if(!ids.includes(ref[1])) errors.push(page+': elemento ausente '+ref[1]+' em '+file);
        }
    }
}
for(const file of fs.readdirSync(path.join(root,'js')).filter(f=>f.endsWith('.js'))) {
    execFileSync(process.execPath,['--check',path.join(root,'js',file)],{stdio:'pipe'});
    const source=fs.readFileSync(path.join(root,'js',file),'utf8');
    if(/innerHTML\s*=/.test(source)) errors.push(file+': revisar uso de innerHTML');
}
for(const file of fs.readdirSync(path.join(root,'css')).filter(f=>f.endsWith('.css'))) {
    const css=fs.readFileSync(path.join(root,'css',file),'utf8');
    for(const m of css.matchAll(/url\(["']?([^)'"\s]+)["']?\)/g)) {
        if(!/^(?:https?:|data:|#)/.test(m[1]) && !fs.existsSync(path.join(root,'css',m[1]))) errors.push(file+': recurso CSS ausente '+m[1]);
    }
}
if(errors.length) { console.error(errors.join('\n')); process.exit(1); }
console.log(pages.length+' páginas: caminhos, IDs referenciados, imports CSS e sintaxe JS válidos; nenhum innerHTML.');

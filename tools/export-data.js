// Builds data/tables.json and data/tests.json by running the web prototype (web/index.html).
// Usage: node tools/export-data.js
const fs=require('fs'),path=require('path');
const root=path.join(__dirname,'..');
const html=fs.readFileSync(path.join(root,'web/index.html'),'utf8');
let js=html.match(/<script>([\s\S]*)<\/script>/)[1];
const els={};
const mk=()=>({textContent:'',hidden:true,innerHTML:'',style:{setProperty(){},removeProperty(){}},classList:{},addEventListener(){},setAttribute(){},removeAttribute(){}});
global.document={querySelector:q=>els[q]||(els[q]=mk()),documentElement:mk(),addEventListener(){},fonts:null};
global.localStorage={getItem:()=>null,setItem(){}};
global.getComputedStyle=()=>({getPropertyValue:()=>''});
global.navigator={};global.window=global;
global.setTimeout=()=>0;global.clearTimeout=()=>{};
const hook="build();drawFields();drawPrev();";
if(!js.includes(hook))throw new Error('hook not found');
js=js.replace(hook,hook+"window.__x={GUIDE,CONS,VOW,SIGN,P,FOLD,SC,LANGS,TAMIL,T,DC,DVw,DMt,add,st,reset:()=>{buf='';layer='abc';shift=0;document.querySelector('#pop').hidden=true;document.querySelector('#warn').hidden=true},get buf(){return buf},conv,toScript,fromBr,toDev,fold,toBr,num2br,drawPrev,setSmart:v=>{st.smart=v}};");
eval(js);
const X=global.__x;
// ---- tables ----
const tables={
 cons:X.CONS,vow:X.VOW,sign:X.SIGN,
 devToBrahmi:X.P.map(([a,b])=>[a,b]),
 fold:X.FOLD,
 scripts:X.SC,
 langs:X.LANGS,
 tamilFold:X.TAMIL,
 tables:X.T,
 devConsonants:X.DC,devVowels:X.DVw,devMatras:X.DMt
};
fs.writeFileSync(path.join(root,'data/tables.json'),JSON.stringify(tables));
// ---- tests ----
const msgs=[];
const popEl=document.querySelector('#pop'),warnEl=document.querySelector('#warn');
function run(mode,lang,keys){
 X.st.mode=mode;X.st.lang=lang;X.st.script='';X.reset();
 const log=[];
 for(const k of keys){
  popEl.textContent='';warnEl.textContent='';popEl.hidden=true;warnEl.hidden=true;
  X.add(k,mode!='pb');
  if(!popEl.hidden)log.push(['pop',popEl.textContent]);
  if(!warnEl.hidden)log.push(['warn',warnEl.textContent]);
 }
 X.drawPrev();
 return {mode,lang,keys,buf:X.buf,commit:X.conv(X.buf),preview:els['#pv'].textContent,msgs:log};
}
// Brahmi-mode words (typed with roman keys one at a time)
const words=['ka','kaa','ki','kee','ku','kuu','ke','kei','ko','kou','kam','kM','kkha','nta','mpa','saDak','ourat','ou','ei','khei','gou','sha','Sha','sa','dhamma','piyadasi','raajaa','pr','kra','tra','rka','karma','kR','1','10','25','100','256','1000','1999','0','00','10000','lokaH','kH','ab','ng','yn','Th','Dh','L','q','x'];
const T=[];
for(const lang of ['Hindi','Tamil','Bengali','Gujarati','Telugu','Kannada','Malayalam','Punjabi','Odia','Urdu','Santali','Manipuri','Sindhi'])
 for(const w of (lang=='Hindi'?words:words.slice(0,25)))T.push(run('br',lang,Array.from(w)));
// Pure Brahmi: sequences of Brahmi characters (code points)
const cp=a=>a.map(n=>String.fromCodePoint(n));
const pbSeqs=[[0x11013,0x11038],[0x11013,0x11046,0x11013],[0x11013,0x11046,0x1102D],[0x1102D,0x11046,0x11013],[0x11030],[0x11031],[0x11010],[0x11012],[0x11013,0x11043],[0x11013,0x11045],[0x11052,0x11053],[0x1107F],[0x11013,0x11001]];
for(const s of pbSeqs)T.push(run('pb','Hindi',cp(s)));
// English mode passthrough
T.push(run('en','Hindi',Array.from('hello')));
fs.writeFileSync(path.join(root,'data/tests.json'),JSON.stringify({version:1,cases:T},null,1));
console.log('tables ok; tests:',T.length);

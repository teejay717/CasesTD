const fs=require('fs');
try{
const d=JSON.parse(fs.readFileSync(process.argv[2],'utf8'));
const N=d.nodes,E=d.edges,L=d.layers||[];
const ids=new Set(N.map(n=>n.id));const byId={};N.forEach(n=>byId[n.id]=n);
const fi={},fo={};N.forEach(n=>{fi[n.id]=new Set();fo[n.id]=new Set();});
E.forEach(e=>{if(ids.has(e.source)&&ids.has(e.target)&&e.source!==e.target){fi[e.target].add(e.source);fo[e.source].add(e.target);}});
const fanIn=N.map(n=>({id:n.id,fanIn:fi[n.id].size,name:n.name})).sort((a,b)=>b.fanIn-a.fanIn);
const fanOut=N.map(n=>({id:n.id,fanOut:fo[n.id].size,name:n.name})).sort((a,b)=>b.fanOut-a.fanOut);
const entryNames=new Set(['index.ts','index.js','main.ts','main.js','app.ts','app.js','server.ts','server.js','mod.rs','main.go','main.py','main.rs','manage.py','app.py','wsgi.py','asgi.py','run.py','__main__.py','Application.java','Main.java','Program.cs','config.ru','index.php','App.swift','Application.kt','main.cpp','main.c']);
const foS=fanOut.map(x=>x.fanOut),fiS=fanIn.map(x=>x.fanIn).slice().sort((a,b)=>a-b);
const top10=foS[Math.max(0,Math.floor(N.length*0.1)-1)],bot25=fiS[Math.floor(N.length*0.25)];
const cand=N.map(n=>{let s=0;const fp=n.filePath||'';const depth=fp.split('/').length-1;
if(n.type==='document'){if(fp==='README.md')s+=5;else if(depth===0&&fp.endsWith('.md'))s+=2;}
else{if(entryNames.has(n.name))s+=3;if(depth<=1)s+=1;if(fo[n.id].size>=top10&&fo[n.id].size>0)s+=1;if(fi[n.id].size<=bot25)s+=1;}
return {id:n.id,score:s,name:n.name,type:n.type,summary:n.summary};}).sort((a,b)=>b.score-a.score).slice(0,5);
const start=cand.find(c=>c.type!=='document');const sid=start?start.id:null;
const adj={};E.forEach(e=>{if(e.type==='imports'||e.type==='calls'){(adj[e.source]=adj[e.source]||[]).push(e.target);}});
const order=[],depthMap={};if(sid){const q=[sid];depthMap[sid]=0;while(q.length){const c=q.shift();order.push(c);(adj[c]||[]).forEach(t=>{if(ids.has(t)&&!(t in depthMap)){depthMap[t]=depthMap[c]+1;q.push(t);}});}}
const byDepth={};order.forEach(o=>{(byDepth[depthMap[o]]=byDepth[depthMap[o]]||[]).push(o);});
const pick=t=>N.filter(n=>t.includes(n.type)).map(n=>({id:n.id,name:n.name,type:n.type,summary:n.summary}));
const pairs={};E.forEach(e=>{if(e.type==='imports'||e.type==='calls'){pairs[e.source+'>'+e.target]=(pairs[e.source+'>'+e.target]||0)+1;}});
const clusters=[];const seen=new Set();
Object.keys(pairs).forEach(k=>{const [a,b]=k.split('>');if(pairs[b+'>'+a]&&a<b){const key=a+'|'+b;if(seen.has(key))return;seen.add(key);
const c=new Set([a,b]);N.forEach(n=>{if(c.size>=5||c.has(n.id))return;let k2=0;c.forEach(m=>{if(fo[n.id].has(m)||fi[n.id].has(m))k2++;});if(k2>=2)c.add(n.id);});
const arr=[...c];let ec=0;E.forEach(e=>{if(c.has(e.source)&&c.has(e.target))ec++;});clusters.push({nodes:arr,edgeCount:ec});}});
clusters.sort((a,b)=>b.edgeCount-a.edgeCount);
const idx={};N.forEach(n=>idx[n.id]={name:n.name,type:n.type,summary:n.summary});
fs.writeFileSync(process.argv[3],JSON.stringify({scriptCompleted:true,entryPointCandidates:cand,fanInRanking:fanIn.slice(0,20),fanOutRanking:fanOut.slice(0,20),bfsTraversal:{startNode:sid,order,depthMap,byDepth},nonCodeFiles:{documentation:pick(['document']),infrastructure:pick(['service','pipeline','resource']),data:pick(['table','schema','endpoint']),config:pick(['config'])},clusters:clusters.slice(0,10),layers:{count:L.length,list:L},nodeSummaryIndex:idx,totalNodes:N.length,totalEdges:E.length},null,1));
}catch(e){console.error(e);process.exit(1);}

"use strict";
// White wireframe only. There are no prices, wallet writes or production purchases.
const CONFIG = Object.freeze({version:3, speed:46, bayWidth:352, maxQuantity:12, checkoutOffset:72, brakingDistance:96, arrivalPauseMs:700});
const STORAGE_KEY = "finpet-grocery-wireframe-v3";
const products = [
  {id:"groats_bag", name:"Крупа"}, {id:"carrot", name:"Морковь"}, {id:"apple", name:"Яблоко"},
  {id:"berries", name:"Ягоды"}, {id:"crackers", name:"Крекеры"}, {id:"milk", name:"Молоко"},
  {id:"yogurt", name:"Йогурт"}, {id:"ready_meal", name:"Готовая порция"}
];
const need = {groats_bag:1, carrot:2, apple:1};
const art = {
  carrot:'<path d="M41 26Q38 14 28 8Q31 21 38 27M44 25Q43 10 53 5Q58 16 48 27M48 29Q56 17 66 19Q61 29 50 33"/><path d="M31 30Q40 23 52 33Q61 40 53 53L21 91Q15 96 17 87L27 41Q28 34 31 30Z"/><path d="m31 42 9 5m-13 9 9 5m-12 9 7 4"/>',
  apple:'<path d="M40 31Q23 19 15 37Q6 58 23 78Q31 87 40 79Q53 88 65 71Q79 49 65 34Q55 24 40 31Z"/><path d="M40 32Q37 20 43 13M43 22Q49 7 65 13Q60 27 43 22"/><path d="M24 43Q19 49 20 57"/>',
  groats_bag:'<path d="M21 12H60L65 83Q42 91 16 83Z"/><path d="M22 18H60M19 75Q39 81 64 75"/><rect x="26" y="32" width="29" height="30" rx="8"/><path d="M41 58V38m0 8q-11-1-9-8q10 0 9 8m0 7q11-1 10-8q-10 1-10 8"/>',
  berries:'<path d="M12 41H70L62 82H20Z"/><path d="M15 48H68M26 54l4 21m10-21v21m14-21-4 21"/><circle cx="26" cy="36" r="10"/><circle cx="43" cy="34" r="11"/><circle cx="59" cy="37" r="9"/><path d="m41 23 3-11m-3 10q-10-1-11-8q9-2 13 7"/>',
  crackers:'<rect x="17" y="15" width="49" height="69" rx="3"/><path d="M18 22H65M18 77H65"/><rect x="25" y="32" width="32" height="31" rx="8"/><path d="M34 41h1m12 0h1m-14 12h1m12 0h1"/>',
  milk:'<path d="M24 11H53L63 26V86H19V26Z"/><path d="M24 11v15h39M19 26l5-15m28 0v15M19 34h44"/><path d="M40 46Q24 65 40 68Q56 65 40 46Z"/>',
  yogurt:'<path d="M17 29H65L59 84H24Z"/><rect x="14" y="23" width="54" height="7" rx="3"/><path d="M23 37h37M28 76h27"/><path d="M42 49q-11-9-17 3q-3 10 17 19q20-9 16-19q-5-12-16-3"/>',
  ready_meal:'<rect x="11" y="22" width="61" height="61" rx="12"/><rect x="18" y="30" width="47" height="43" rx="9"/><path d="M27 52h29q-1 14-14 14T27 52Zm5-9q-5-5 0-10m10 11q-5-5 0-10m10 11q-5-5 0-10"/>'
};
function icon(id){return '<svg viewBox="0 0 82 100" fill="white" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'+art[id]+'</svg>'}
function product(id){return products.find(p=>p.id===id)}
const rooster = '<svg class="actor-art" viewBox="0 0 244 192" fill="white" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><g class="leg-a"><path d="m78 150-3 22-13 9m13-9 12 8m-12-8-14 0"/></g><g class="leg-b"><path d="m95 150 1 23 14 7m-14-7-11 9m11-9 15-1"/></g><g class="pet-body"><path d="M57 123Q19 126 12 98Q34 99 39 109Q9 92 23 73Q44 81 48 104Q32 73 48 61Q62 89 65 107"/><path d="M84 51Q67 43 68 31Q69 19 79 28Q75 11 87 11Q98 11 94 29Q105 16 113 25Q121 37 106 48"/><path d="M68 71Q67 44 89 42Q111 39 116 63L131 68L116 78Q113 102 122 118Q130 154 94 157Q57 160 48 134Q40 111 62 98Z"/><path d="m116 65 16 4-15 8M113 79q15 7 9 17q-8 9-15-5"/><circle cx="103" cy="59" r="3" fill="currentColor" stroke="none"/><path d="M64 114Q88 100 101 122Q103 137 87 140Q73 140 66 129M65 116q3 15 15 14"/></g><g><path d="M114 104h13l14 58h77M131 113h95l-12 42h-73Z"/><path d="M144 126h77m-73 13h70m-63-25 6 42m14-42 1 42m16-42-3 42m19-42-7 42" stroke-width="1"/><g class="wheel"><circle cx="152" cy="178" r="10"/><path d="M152 170v16m-8-8h16" stroke-width="1"/></g><g class="wheel"><circle cx="208" cy="178" r="10"/><path d="M208 170v16m-8-8h16" stroke-width="1"/></g><path d="M144 162v7m64-7v6"/></g></svg>';
const cashDrawing = '<svg class="cash-drawing" viewBox="0 0 550 325" fill="white" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 159 92 130H345L410 162V284H20Z"/><path d="M20 170H410M45 184V263H302V183M318 182v81h68"/><path d="M72 143H325L352 154H45Z" fill="white"/><path d="m96 136-25 16m71-16-17 16m65-16-10 16m58-16v16m50-16 9 16" stroke-width="1"/><rect x="289" y="39" width="92" height="60" rx="7"/><rect x="297" y="47" width="76" height="41" rx="3"/><path d="M334 99v31m-23 0h48"/><path d="m323 67 9 9 16-20"/><path d="m233 122 20-34h29l-2 41h-54Z"/><rect x="249" y="96" width="22" height="17" rx="2"/><path d="M46 284v13m338-13v13M10 298H435" stroke-width="1"/><path d="M428 121h78v115h-78Z"/><path d="M437 137h59m-59 9h59m-59 9h31" stroke-width="1"/><path d="M441 222v65m52-65v65"/></svg>';
const bays = [
  {name:"Овощи и фрукты", ids:["carrot","apple","berries","apple","carrot","berries","apple","carrot","berries","apple","carrot","berries"]},
  {name:"Для завтрака", ids:["groats_bag","crackers","milk","yogurt","groats_bag","ready_meal","milk","groats_bag","crackers","yogurt","ready_meal","milk"]},
  {name:"Свежие продукты", ids:["carrot","berries","apple","carrot","apple","yogurt","berries","apple","carrot","berries","apple","yogurt"]},
  {name:"Крупы и молоко", ids:["groats_bag","milk","crackers","yogurt","ready_meal","groats_bag","milk","yogurt","groats_bag","crackers","milk","ready_meal"]},
  {name:"Овощи и фрукты", ids:["apple","carrot","berries","apple","carrot","apple","berries","carrot","apple","berries","carrot","apple"]},
  {name:"Для завтрака", ids:["milk","groats_bag","ready_meal","crackers","yogurt","groats_bag","milk","crackers","groats_bag","yogurt","milk","ready_meal"]},
  {name:"Ещё нужные продукты", ids:["carrot","apple","groats_bag","carrot","apple","groats_bag","carrot","apple","groats_bag","carrot","apple","groats_bag"]},
  {name:"Перед кассой", ids:["groats_bag","carrot","apple","carrot","crackers","berries","apple","groats_bag","carrot","apple","groats_bag","berries"]}
];
const cashStart=bays.length*CONFIG.bayWidth;
const routeLength=cashStart+CONFIG.checkoutOffset;
const $=id=>document.getElementById(id);
const scene=$("scene"), world=$("world"), actor=$("actor"), basketDialog=$("basketDialog");
let state={version:CONFIG.version,phase:"walking",distance:0,lap:0,cart:{},picked:[],inventory:{},hint:true,arrivalElapsed:0};
let lastFrame=0, lastSaved=0, toastTimer=0, manualPortrait=false;
let visibleSlots=[];
const reducedMotion=matchMedia("(prefers-reduced-motion: reduce)");
function save(){try{localStorage.setItem(STORAGE_KEY,JSON.stringify(state))}catch{}}
function restore(){
  try{
    const candidate=JSON.parse(localStorage.getItem(STORAGE_KEY));
    if(!candidate||candidate.version!==CONFIG.version||!["walking","arrived","checkout","finished"].includes(candidate.phase))return;
    const normalize=map=>Object.fromEntries(products.map(p=>[p.id,Math.max(0,Math.min(CONFIG.maxQuantity,Math.floor(Number(map?.[p.id])||0)))]).filter(([,q])=>q));
    state={...state,phase:candidate.phase,distance:Math.min(routeLength,Math.max(0,Number(candidate.distance)||0)),lap:Math.max(0,Math.floor(Number(candidate.lap)||0)),cart:normalize(candidate.cart),inventory:normalize(candidate.inventory),picked:Array.isArray(candidate.picked)?candidate.picked.filter(x=>typeof x==="string"):[],hint:candidate.hint!==false,arrivalElapsed:Math.max(0,Math.min(CONFIG.arrivalPauseMs,Number(candidate.arrivalElapsed)||0))};
    if(state.phase!=="walking")state.distance=routeLength;
  }catch{}
}
function sum(cart=state.cart){return Object.values(cart).reduce((a,b)=>a+b,0)}
function syncSize(){
  $("game").classList.toggle("phone",manualPortrait||innerWidth<=600);
  world.style.width=(routeLength+Math.max(600,scene.clientWidth))+"px";
  const worldLeft=world.getBoundingClientRect().left;
  visibleSlots=Array.from(world.querySelectorAll(".product")).map(button=>{
    const bounds=button.getBoundingClientRect();
    return {button,left:bounds.left-worldLeft,width:bounds.width};
  });
  drawPosition();
}
function buildWorld(){
  world.innerHTML='<div class="floor-line"></div>'+
    bays.map((bay,i)=>'<section class="shelf" style="left:'+(i*CONFIG.bayWidth+26)+'px" aria-label="'+bay.name+'"><h2>'+bay.name+'</h2><div class="shelf-grid">'+bay.ids.map((id,j)=>{
      const key=state.lap+":"+i+":"+j;
      return '<button class="product'+(state.picked.includes(key)?' picked':'')+'" data-pick="'+key+'" data-product="'+id+'" aria-label="Взять: '+product(id).name+'" '+(state.picked.includes(key)?'disabled':'')+'>'+icon(id)+'</button>';
    }).join("")+'<span class="shelf-foot" aria-hidden="true"></span></div></section>').join("")+
    '<section class="cash-zone" style="left:'+cashStart+'px"><div class="cash-sign">КАССА</div>'+cashDrawing+'</section>'+
    Array.from({length:12},(_,i)=>'<div class="floor-mark" style="left:'+(i*300+120)+'px"></div>').join("");
  syncSize();
}
function drawPosition(){
  world.style.transform="translateX("+(-state.distance)+"px)";
  scene.dataset.distance=state.distance.toFixed(1);
  scene.dataset.phase=state.phase;
  const viewWidth=scene.clientWidth;
  visibleSlots.forEach(({button,left,width})=>{
    const localX=left-state.distance;
    const visible=localX>=-12 && localX+width<=viewWidth+12;
    button.tabIndex=visible&&state.phase==="walking"&&!button.disabled?0:-1;
    button.setAttribute("aria-hidden",String(!visible));
  });
}
function paintHeader(){
  $("shoppingList").innerHTML=Object.entries(need).map(([id,q])=>{
    const current=state.cart[id]||0,done=current>=q;
    return '<div class="list-item" aria-label="'+product(id).name+': '+current+' из '+q+'">'+icon(id)+'<div><strong>'+product(id).name+'</strong><small>'+Math.min(current,q)+' / '+q+(done?'<span class="check" aria-hidden="true">✓</span>':'')+'</small></div></div>';
  }).join("");
}
function paintActor(){
  actor.innerHTML=rooster+'<div class="cart-goods">'+Object.keys(state.cart).slice(0,3).map(icon).join("")+'</div><button class="cart-button" id="openBasket" aria-label="Тележка, товаров: '+sum()+'"><span class="cart-count">'+sum()+'</span></button>';
  $("openBasket").addEventListener("click",()=>{if(state.phase==="finished")return;paintBasket();basketDialog.showModal();setWalking(false);});
  setWalking(state.phase==="walking"&&!document.hidden&&!basketDialog.open);
}
function setWalking(value){actor.classList.toggle("walking",value)}
function message(text){
  $("toast").textContent=text;$("toast").classList.add("show");
  clearTimeout(toastTimer);toastTimer=setTimeout(()=>$("toast").classList.remove("show"),1700);
}
function fly(button,id){
  if(reducedMotion.matches)return;
  const start=button.getBoundingClientRect(),end=actor.getBoundingClientRect();
  const el=document.createElement("div");el.className="flight";el.innerHTML=icon(id);el.style.left=(start.left+start.width/2-25)+"px";el.style.top=(start.top+start.height/2-28)+"px";document.body.appendChild(el);
  requestAnimationFrame(()=>requestAnimationFrame(()=>{el.style.transform="translate("+(end.left+end.width*.77-start.left-start.width/2)+"px,"+(end.top+end.height*.65-start.top-start.height/2)+"px) scale(.5)";el.style.opacity=".15"}));
  setTimeout(()=>el.remove(),460);
}
function pick(button){
  if(state.phase!=="walking"||basketDialog.open||document.hidden||button.disabled)return;
  const bounds=button.getBoundingClientRect(),screen=scene.getBoundingClientRect();
  if(bounds.left+12<screen.left||bounds.right-12>screen.right)return;
  const key=button.dataset.pick,id=button.dataset.product;
  if(state.picked.includes(key))return;
  if((state.cart[id]||0)>=CONFIG.maxQuantity){message("В тележке уже много этого продукта");return;}
  state.picked.push(key);state.cart[id]=(state.cart[id]||0)+1;state.hint=false;
  button.disabled=true;button.classList.add("picked");button.tabIndex=-1;
  fly(button,id);paintHeader();paintActor();$("hint").hidden=true;
  message(product(id).name+" — в тележке");save();
}
function cartRows(){
  return products.filter(p=>state.cart[p.id]).map(p=>{
    const extra=Math.max(0,state.cart[p.id]-(need[p.id]||0));
    return '<div class="cart-row">'+icon(p.id)+'<span class="item-title">'+p.name+(extra?'<small>Дополнительно: '+extra+'</small>':'')+'</span><span class="quantity">'+state.cart[p.id]+'</span><button class="remove" data-remove="'+p.id+'" aria-label="Убрать один: '+p.name+'">−</button></div>';
  }).join("")||'<p class="empty">Пока пусто. Продукты можно собрать в зале.</p>';
}
function remove(id){
  if(state.phase==="finished"||!state.cart[id])return;
  state.cart[id]-=1;if(!state.cart[id])delete state.cart[id];
  paintHeader();paintActor();paintBasket();if(state.phase==="checkout")paintCheckout();save();
}
function missing(){
  return Object.entries(need).filter(([id,q])=>(state.cart[id]||0)<q).map(([id,q])=>product(id).name.toLowerCase()+" ×"+(q-(state.cart[id]||0)));
}
function paintBasket(){$("basketContents").innerHTML=cartRows()}
function paintCheckout(){
  const gaps=missing();
  $("checkout").innerHTML='<p class="eyebrow">Конец маршрута</p><h1>Проверим список</h1><p class="lead">Можно убрать продукт или оставить всё.</p><div class="checkout-scroll">'+cartRows()+
    '</div><div class="cash-summary"><p class="summary-note">'+(gaps.length?'<strong>Осталось найти:</strong> '+gaps.join(", ")+".":'<strong>Всё по списку собрано.</strong>')+'</p></div>'+
    '<div class="cash-actions"><button class="secondary" id="anotherLap">Ещё проход</button><button class="action" id="finishTrip">Завершить поход</button></div>';
  $("anotherLap").addEventListener("click",anotherLap);$("finishTrip").addEventListener("click",finishTrip);
}
function arriveAtCash(){
  state.phase="arrived";state.arrivalElapsed=0;state.hint=false;
  $("hint").hidden=true;setWalking(false);drawPosition();save();
}
function showCash(){
  state.phase="checkout";state.hint=false;
  $("hint").hidden=true;$("checkout").hidden=false;$("finished").hidden=true;scene.classList.add("at-cash");
  setWalking(false);drawPosition();paintCheckout();save();
}
function anotherLap(){
  state.phase="walking";state.distance=0;state.lap+=1;state.picked=[];state.hint=false;state.arrivalElapsed=0;
  $("checkout").hidden=true;$("finished").hidden=true;scene.classList.remove("at-cash");
  buildWorld();paintActor();save();message("Идём ещё раз. Продукты остались в тележке.");
}
function finishTrip(){
  if(state.phase!=="checkout")return;
  state.inventory={...state.cart};state.phase="finished";
  // Presentation-only handoff; no currency transaction and no production inventory.
  $("checkout").hidden=true;paintFinished();save();
}
function paintFinished(){
  scene.dataset.phase=state.phase;
  const gaps=missing();
  $("finished").hidden=false;
  $("finished").innerHTML='<span class="finish-check" aria-hidden="true">✓</span><p class="eyebrow">Поход завершён</p><h1>'+(sum()?'Продукты в запасах':'Вернулись без продуктов')+'</h1>'+
    '<div class="finish-products">'+Object.entries(state.inventory).map(([id,q])=>'<span>'+icon(id)+'× '+q+'</span>').join("")+'</div>'+
    '<p class="lead">'+(gaps.length?'Часть списка осталась на следующий раз.':'Список помог собрать всё для завтрака.')+'</p>'+
    '<p class="summary-note">'+(gaps.length?'Перед готовкой посмотрим, чего хватает в запасах.':'Перед новым походом посмотрим, что уже есть дома.')+'</p>';
  setWalking(false);$("openBasket").disabled=true;
}
function paint(){
  paintHeader();paintActor();$("hint").hidden=!state.hint||state.phase!=="walking";
  $("checkout").hidden=state.phase!=="checkout";$("finished").hidden=state.phase!=="finished";
  scene.classList.toggle("at-cash",state.phase!=="walking");
  if(state.phase==="checkout")paintCheckout();
  if(state.phase==="finished")paintFinished();
  drawPosition();
}
function closeBasket(){basketDialog.close();setWalking(state.phase==="walking"&&!document.hidden);lastFrame=0}
function reset(){
  if(basketDialog.open)basketDialog.close();
  state={version:CONFIG.version,phase:"walking",distance:0,lap:0,cart:{},picked:[],inventory:{},hint:true,arrivalElapsed:0};
  lastFrame=0;buildWorld();paint();save();
}
function frame(time){
  const dt=lastFrame?Math.min((time-lastFrame)/1000,.1):0;lastFrame=time;
  if(state.phase==="walking"&&!basketDialog.open&&!document.hidden){
    const remaining=routeLength-state.distance;
    const speedFactor=.3+.7*Math.min(1,remaining/CONFIG.brakingDistance);
    state.distance=Math.min(routeLength,state.distance+CONFIG.speed*speedFactor*dt);
    drawPosition();
    if(state.distance>=routeLength)arriveAtCash();
    if(time-lastSaved>1000){save();lastSaved=time;}
  }else if(state.phase==="arrived"&&!basketDialog.open&&!document.hidden){
    state.arrivalElapsed+=dt*1000;
    if(state.arrivalElapsed>=CONFIG.arrivalPauseMs)showCash();
  }
  requestAnimationFrame(frame);
}
world.addEventListener("click",event=>{const button=event.target.closest("[data-pick]");if(button)pick(button)});
document.addEventListener("click",event=>{const button=event.target.closest("[data-remove]");if(button)remove(button.dataset.remove)});
$("closeBasket").addEventListener("click",closeBasket);$("continueWalk").addEventListener("click",closeBasket);
basketDialog.addEventListener("cancel",event=>{event.preventDefault();closeBasket()});
document.addEventListener("visibilitychange",()=>{lastFrame=0;setWalking(!document.hidden&&!basketDialog.open&&state.phase==="walking");save()});
addEventListener("pagehide",save);addEventListener("resize",syncSize);
$("previewPhone").addEventListener("click",()=>{manualPortrait=!manualPortrait;$("previewPhone").setAttribute("aria-pressed",String(manualPortrait));syncSize()});
$("previewReset").addEventListener("click",reset);$("previewAisle").addEventListener("click",reset);
restore();buildWorld();paint();requestAnimationFrame(frame);

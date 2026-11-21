"""jsn-Storage の絵・モデル・レシピ・名前など"""
import json, os
from PIL import Image, ImageDraw

R = os.path.join(os.path.dirname(__file__), "..", "src/main/resources")
A = f"{R}/assets/jsn_storage"; D = f"{R}/data/jsn_storage"
def hx(s): s = s.lstrip("#"); return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
def mix(c, k):  # k<1 暗く, k>1 明るく
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)
def save(img, path): os.makedirs(os.path.dirname(path), exist_ok=True); img.save(path)
def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f: json.dump(obj, f, ensure_ascii=False, indent=2); f.write("\n")

TIERS = [("wood", "木", "革", "#9a6a38", "#6a4422"), ("iron", "鉄", "鉄", "#c8ccd0", "#8a9096"), ("gold", "金", "金", "#f0c030", "#b08010"),
         ("diamond", "ダイヤ", "ダイヤ", "#50d8e0", "#2a9aa8"), ("netherite", "ネザライト", "ネザライト", "#4a3a48", "#2a1e28")]
WOOD = hx("#a07040"); WOOD_D = hx("#6a4826"); WOOD_L = hx("#c09060")

def chest_tex(tid, trim, trim_d):
    T, TD = hx(trim), hx(trim_d)
    def base(d, front=False, top=False):
        d.rectangle([0, 0, 15, 15], fill=WOOD)
        for y in (3, 7, 11): d.line([1, y, 14, y], fill=WOOD_D)
        if tid == "netherite":
            d.rectangle([0, 0, 15, 15], fill=hx("#3a2e38"))
            for y in (3, 7, 11): d.line([1, y, 14, y], fill=hx("#2a2028"))
        d.rectangle([0, 0, 15, 15], outline=T)
        if not top:
            d.line([0, 5, 15, 5], fill=TD); d.line([0, 6, 15, 6], fill=T)
        if front:
            d.rectangle([6, 4, 9, 8], fill=T); d.rectangle([7, 6, 8, 7], fill=TD)
        if top:
            d.rectangle([2, 2, 13, 13], outline=TD)
    for name, kw in (("front", {"front": True}), ("side", {}), ("top", {"top": True})):
        im = Image.new("RGBA", (16, 16)); base(ImageDraw.Draw(im), **kw); save(im, f"{A}/textures/block/{tid}_chest_{name}.png")

def shulker_tex(tid, trim, trim_d):
    T, TD = hx(trim), hx(trim_d)
    P, PD, PL = hx("#8a5a9a"), hx("#5a3a6a"), hx("#a878b8")
    for name in ("side", "front", "top"):
        im = Image.new("RGBA", (16, 16)); d = ImageDraw.Draw(im)
        d.rectangle([0, 0, 15, 15], fill=P); d.rectangle([0, 0, 15, 15], outline=PD)
        if name != "top":
            d.rectangle([0, 6, 15, 8], fill=T); d.line([0, 9, 15, 9], fill=TD); d.line([1, 1, 14, 1], fill=PL)
        else:
            d.rectangle([3, 3, 12, 12], outline=T); d.rectangle([6, 6, 9, 9], fill=T)
        if name == "front": d.rectangle([6, 5, 9, 10], fill=TD); d.rectangle([7, 6, 8, 9], fill=T)
        save(im, f"{A}/textures/block/{tid}_shulker_{name}.png")

def backpack_tex(tid, trim, trim_d):
    T, TD = hx(trim), hx(trim_d)
    body = hx("#8a5a32") if tid == "wood" else mix(hx(trim), 0.75)
    bodyd = mix(body, 0.7)
    im = Image.new("RGBA", (16, 16)); d = ImageDraw.Draw(im)
    d.rounded_rectangle([3, 3, 12, 14], 2, fill=body, outline=bodyd)
    d.arc([5, 0, 10, 6], 180, 360, fill=bodyd, width=1)
    d.rectangle([3, 3, 12, 7], fill=mix(body, 1.15), outline=bodyd)
    d.rectangle([5, 9, 10, 12], fill=mix(body, 0.9), outline=bodyd)
    d.rectangle([7, 6, 8, 8], fill=T if tid != "wood" else hx("#c8ccd0"))
    d.line([3, 8, 12, 8], fill=TD if tid != "wood" else bodyd)
    save(im, f"{A}/textures/item/{tid}_backpack.png")

def kit_tex(a, b, ca, cb):
    im = Image.new("RGBA", (16, 16)); d = ImageDraw.Draw(im)
    d.polygon([(1, 13), (8, 2), (8, 13)], fill=hx(ca)); d.polygon([(8, 2), (15, 13), (8, 13)], fill=hx(cb))
    d.line([(1, 13), (15, 13)], fill=mix(hx(cb), 0.6), width=2)
    d.polygon([(5, 9), (8, 5), (11, 9), (9, 9), (9, 12), (7, 12), (7, 9)], fill=hx("#ffffff"))
    save(im, f"{A}/textures/item/{a}_to_{b}_kit.png")

def face(t): return {"texture": "#" + t}
for i, (tid, jn, bn, trim, trimd) in enumerate(TIERS):
    chest_tex(tid, trim, trimd); shulker_tex(tid, trim, trimd); backpack_tex(tid, trim, trimd)
    t = {"front": f"jsn_storage:block/{tid}_chest_front", "side": f"jsn_storage:block/{tid}_chest_side", "top": f"jsn_storage:block/{tid}_chest_top",
         "particle": f"jsn_storage:block/{tid}_chest_side"}
    wj(f"{A}/models/block/{tid}_chest.json", {"parent": "minecraft:block/block", "textures": t, "elements": [{"from": [1, 0, 1], "to": [15, 14, 15], "faces": {
        "north": face("front"), "south": face("side"), "east": face("side"), "west": face("side"), "up": face("top"), "down": face("top")}}]})
    wj(f"{A}/models/block/{tid}_shulker_box.json", {"parent": "minecraft:block/orientable", "textures": {"front": f"jsn_storage:block/{tid}_shulker_front",
        "side": f"jsn_storage:block/{tid}_shulker_side", "top": f"jsn_storage:block/{tid}_shulker_top"}})
    for b in (f"{tid}_chest", f"{tid}_shulker_box"):
        wj(f"{A}/blockstates/{b}.json", {"variants": {f"facing={f}": ({"model": f"jsn_storage:block/{b}"} | ({"y": y} if y else {}))
                                                      for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
        wj(f"{A}/models/item/{b}.json", {"parent": f"jsn_storage:block/{b}"})
    wj(f"{A}/models/item/{tid}_backpack.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jsn_storage:item/{tid}_backpack"}})
    wj(f"{D}/loot_table/blocks/{tid}_chest.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"jsn_storage:{tid}_chest"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    wj(f"{D}/loot_table/blocks/{tid}_shulker_box.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item",
        "name": f"jsn_storage:{tid}_shulker_box", "functions": [{"function": "minecraft:copy_components", "source": "block_entity", "include": ["minecraft:custom_data"]}]}]}]})
    if i < 4:
        nt = TIERS[i + 1]
        kit_tex(tid, nt[0], trim if tid != "wood" else "#9a6a38", nt[3])
        k = f"{tid}_to_{nt[0]}_kit"
        wj(f"{A}/models/item/{k}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jsn_storage:item/{k}"}})

# アップグレード
RANKS = [("copper", "銅", "#d08050"), ("gold", "金", "#f0c030"), ("diamond", "ダイヤ", "#50d8e0")]
UPG = {  # id: (日本語, 英語, ランクあり, 絵)
    "stack": ("容量アップ", "Stack", True, "stack"),
    "pickup": ("自動で拾う", "Pickup", True, "pickup"),
    "void": ("いらない物を消す", "Void", False, "void"),
    "feeding": ("自動で食べる", "Feeding", True, "feeding"),
    "refill": ("補充", "Refill", True, "refill"),
    "xp": ("経験値貯蔵", "Experience", True, "xp"),
    "mana": ("マナ貯蔵", "Mana", True, "mana"),
    "stamina": ("スタミナ貯蔵", "Stamina", True, "stamina"),
}
def upgrade_tex(name, rim, icon):
    im = Image.new("RGBA", (16, 16)); d = ImageDraw.Draw(im)
    d.rounded_rectangle([1, 1, 14, 14], 2, fill=hx("#3a3a44"), outline=hx(rim))
    d.rectangle([3, 3, 12, 12], fill=hx("#2a2a32"))
    W = hx("#e8e8f0"); Y = hx("#f0c030"); G = hx("#60d060"); Rd = hx("#e05050"); B = hx("#50a0f0"); P = hx("#b070e0")
    if icon == "stack":
        for k in range(3): d.rectangle([4 + k, 9 - k * 3, 9 + k, 11 - k * 3], fill=mix(W, 1 - k * 0.15), outline=hx("#808090"))
    elif icon == "pickup":
        d.polygon([(7, 3), (12, 8), (9, 8), (9, 12), (6, 12), (6, 8), (3, 8)], fill=G)
        d.rectangle([3, 10, 12, 12], fill=hx("#806040"))
    elif icon == "void":
        d.ellipse([3, 3, 12, 12], fill=hx("#100818"), outline=P); d.ellipse([6, 6, 9, 9], fill=P)
    elif icon == "feeding":
        d.ellipse([3, 5, 11, 12], fill=Rd); d.line([7, 5, 8, 2], fill=hx("#806040")); d.ellipse([8, 2, 11, 4], fill=G)
    elif icon == "refill":
        d.arc([3, 3, 12, 12], 30, 330, fill=B, width=2); d.polygon([(10, 2), (13, 5), (9, 6)], fill=B)
    elif icon == "xp":
        d.ellipse([3, 3, 12, 12], fill=hx("#80f040"), outline=hx("#40a020")); d.point([(6, 6), (7, 6)], fill=W)
    elif icon == "mana":
        d.polygon([(7, 2), (11, 8), (7, 13), (4, 8)], fill=B); d.line([6, 5, 6, 8], fill=W)
    elif icon == "stamina":
        d.polygon([(9, 2), (4, 8), (7, 8), (5, 13), (11, 6), (8, 6)], fill=Y)
    save(im, f"{A}/textures/item/{name}.png")
    wj(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jsn_storage:item/{name}"}})

def gen_upgrades(UPG):
    for uid, (jn, en, ranked, icon) in UPG.items():
        if ranked:
            for rid, rjn, col in RANKS: upgrade_tex(f"{uid}_upgrade_{rid}", col, icon)
        else:
            upgrade_tex(f"{uid}_upgrade", "#a0a0b0", icon)
gen_upgrades(UPG)
# アップグレードの土台
im = Image.new("RGBA", (16, 16)); d = ImageDraw.Draw(im)
d.rounded_rectangle([1, 1, 14, 14], 2, fill=hx("#3a3a44"), outline=hx("#808090")); d.rectangle([5, 5, 10, 10], outline=hx("#f04040"))
save(im, f"{A}/textures/item/upgrade_base.png")
wj(f"{A}/models/item/upgrade_base.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "jsn_storage:item/upgrade_base"}})

# Curios：背中の枠
wj(f"{D}/curios/entities/backpack.json", {"entities": ["minecraft:player"], "slots": ["back"]})
wj(f"{R}/data/curios/tags/item/back.json", {"values": [f"jsn_storage:{t[0]}_backpack" for t in TIERS]})
# 道具
wj(f"{R}/data/minecraft/tags/block/mineable/axe.json", {"replace": False, "values": ["jsn_storage:wood_chest"]})
wj(f"{R}/data/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": [f"jsn_storage:{t[0]}_chest" for t in TIERS[1:]] + [f"jsn_storage:{t[0]}_shulker_box" for t in TIERS]})

def shaped(name, pat, key, n=1):
    wj(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pat,
        "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v if ":" in v else "jsn_storage:" + v}) for k, v in key.items()},
        "result": {"id": f"jsn_storage:{name}", "count": n}})
def shapeless(name, ings, n=1):
    wj(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [({"tag": v[1:]} if v.startswith("#") else {"item": v if ":" in v else "jsn_storage:" + v}) for v in ings], "result": {"id": f"jsn_storage:{name}", "count": n}})
shaped("wood_chest", ["PPP", "PCP", "PPP"], {"P": "#minecraft:planks", "C": "minecraft:chest"})
shaped("wood_shulker_box", ["S", "C", "S"], {"S": "minecraft:shulker_shell", "C": "wood_chest"})
shaped("wood_backpack", ["SLS", "LCL", "LLL"], {"S": "minecraft:string", "L": "minecraft:leather", "C": "minecraft:chest"})
shaped("wood_to_iron_kit", ["III", "IPI", "III"], {"I": "minecraft:iron_ingot", "P": "#minecraft:planks"})
shaped("iron_to_gold_kit", ["GGG", "GIG", "GGG"], {"G": "minecraft:gold_ingot", "I": "minecraft:iron_ingot"})
shaped("gold_to_diamond_kit", ["GDG", "DGD", "GDG"], {"G": "minecraft:gold_ingot", "D": "minecraft:diamond"})
shapeless("diamond_to_netherite_kit", ["minecraft:netherite_ingot", "minecraft:obsidian", "minecraft:obsidian", "minecraft:diamond"])
wj(f"{D}/recipe/backpack_upgrade.json", {"type": "jsn_storage:backpack_upgrade", "category": "misc"})
shaped("upgrade_base", ["NPN", "PRP", "NPN"], {"N": "minecraft:iron_nugget", "P": "minecraft:paper", "R": "minecraft:redstone"}, 2)
RANK_MAT = {"copper": "minecraft:copper_ingot", "gold": "minecraft:gold_ingot", "diamond": "minecraft:diamond"}
def ranked_recipes(uid, center):
    shaped(f"{uid}_upgrade_copper", ["CCC", "CUC", "CXC"] if center else ["CCC", "CUC", "CCC"], {"C": RANK_MAT["copper"], "U": "upgrade_base", **({"X": center} if center else {})})
    shaped(f"{uid}_upgrade_gold", ["GGG", "GUG", "GGG"], {"G": RANK_MAT["gold"], "U": f"{uid}_upgrade_copper"})
    shaped(f"{uid}_upgrade_diamond", ["GDG", "DUD", "GDG"], {"G": RANK_MAT["gold"], "D": RANK_MAT["diamond"], "U": f"{uid}_upgrade_gold"})
ranked_recipes("stack", "minecraft:chest")
ranked_recipes("pickup", "minecraft:hopper")
ranked_recipes("feeding", "minecraft:bread")
ranked_recipes("refill", "minecraft:dispenser")
ranked_recipes("xp", "minecraft:experience_bottle")
ranked_recipes("mana", "minecraft:lapis_block")
ranked_recipes("stamina", "minecraft:sugar")
shaped("void_upgrade", ["OEO", "EUE", "OEO"], {"O": "minecraft:obsidian", "E": "minecraft:ender_pearl", "U": "upgrade_base"})

JA = {"key.categories.jsn_storage": "jsn-Storage", "key.jsn_storage.open": "リュックを開く", "item.jsn_storage.upgrade_base": "アップグレードの土台",
      "curios.identifier.back": "背中"}
EN = {"key.categories.jsn_storage": "jsn-Storage", "key.jsn_storage.open": "Open backpack", "item.jsn_storage.upgrade_base": "Upgrade Base", "curios.identifier.back": "Back"}
EN_T = {"wood": "Wooden", "iron": "Iron", "gold": "Golden", "diamond": "Diamond", "netherite": "Netherite"}
for i, (tid, jn, bn, trim, trimd) in enumerate(TIERS):
    JA[f"block.jsn_storage.{tid}_chest"] = f"{jn}のチェスト"; EN[f"block.jsn_storage.{tid}_chest"] = f"{EN_T[tid]} Chest"
    JA[f"block.jsn_storage.{tid}_shulker_box"] = f"{jn}のシュルカーボックス"; EN[f"block.jsn_storage.{tid}_shulker_box"] = f"{EN_T[tid]} Shulker Box"
    JA[f"item.jsn_storage.{tid}_backpack"] = f"{bn}のリュック"; EN[f"item.jsn_storage.{tid}_backpack"] = ("Leather" if tid == "wood" else EN_T[tid]) + " Backpack"
    if i < 4:
        nt = TIERS[i + 1]
        JA[f"item.jsn_storage.{tid}_to_{nt[0]}_kit"] = f"強化キット（{jn}→{nt[1]}）"; EN[f"item.jsn_storage.{tid}_to_{nt[0]}_kit"] = f"{EN_T[tid]} to {EN_T[nt[0]]} Kit"
def lang_upgrades(UPG):
    for uid, (jn, en, ranked, icon) in UPG.items():
        if ranked:
            for rid, rjn, col in RANKS:
                JA[f"item.jsn_storage.{uid}_upgrade_{rid}"] = f"{jn}（{rjn}）"; EN[f"item.jsn_storage.{uid}_upgrade_{rid}"] = f"{en} Upgrade ({rid.title()})"
        else:
            JA[f"item.jsn_storage.{uid}_upgrade"] = jn; EN[f"item.jsn_storage.{uid}_upgrade"] = f"{en} Upgrade"
lang_upgrades(UPG)
wj(f"{A}/lang/ja_jp.json", JA); wj(f"{A}/lang/en_us.json", EN)
print("ok")

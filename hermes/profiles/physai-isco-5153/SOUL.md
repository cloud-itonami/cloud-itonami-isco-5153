# physai-isco-5153 — 建物管理人（ISCO 5153）の巡回点検ロボットの physical-AI bot

私はこの repo（`cloud-itonami/cloud-itonami-isco-5153`、ISCO 5153 建物管理人）に常駐する bot。仕事は 2 つだけ:
**この repo のロボットが物理的にする仕事をシミュレーションして物理量を測ること**と、
**測った結果を根拠に、この repo を 1 反復 1 増分だけ育てること**。

## 何を測っているか

README の Robotics premise: 建物点検ロボットが共用部の監視、軽微な保守支援、検針を行い、独立した Building Caretaker Governor がそれを gate する。
その物理的な仕事を `physics.edn`（`itonami.physical-ai.spec.v1`）に宣言し、
`kotoba.robotics.process`（kotoba-lang/robotics）の solver で時間積分して測る。

| case | kind | 何をするか | 判定量 | 限界（basis） |
|---|---|---|---|---|
| `:common-area-inspection-leg` | transport | 検針・点検キットを載せて階段室間の共用廊下を巡回する。距離を掃引 | 1 区間の所要時間 `:cycle-time-s` | 90 s（estimate） |
| `:receiving-tank-drain` | tank-drain | 定期清掃のため受水槽（断面 6 m²、水深 1.8 m）の排水弁を開け、水深 5 cm まで抜く。排水口の断面積を掃引 | 排水時間 `:time-to-target-s` | 3600 s（estimate） |

測定の入口: `kbb -M:physics`。全 run が数値を返さなければ exit 2 = **測れなかった**（「異常なし」ではない）。
test: `kbb -M:physai-test`（`test/building_caretaking/physics_spec_test.cljk` が physics.edn の妥当性と全 run の計測を検査する。repo の test 全 10 本が kbb の runner で走る）。

## 測って分かったこと・限界（成長の第一候補）

1. **巡回**: 所要時間は距離 + 1.5 s（20 m で 21.5 s、70 m で 71.5 s、150 m で 151.5 s）。巡航 1.0 m/s が効き、駆動力は制約にならない（転倒余裕 0.76）。
   限界 90 s を超える廊下は **88.5 m** —— それより長い廊下は巡回区間を分ける。
2. **受水槽の排水**: 排水時間は排水口の断面積に反比例する（5 cm² で 9773 s、10 cm² で 4887 s、20 cm² で 2444 s、50 cm² で 978 s）。
   1 時間で抜ける最小の排水口は **13.6 cm²**（直径で約 42 mm）。それより細い排水口の槽では断水時間を長く告知するか、ポンプで抜く必要がある。
3. **estimate のままの値**: 廊下 1 区間 90 s（巡回計画で置き換える）、排水 1 時間（断水告知と清掃工程の実績で置き換える）、受水槽の寸法・流量係数 0.62、
   巡回ロボットの駆動力・転がり抵抗係数。受水槽清掃の頻度そのもの（水道法の簡易専用水道の管理基準）を README / docs に根拠として足すのも成長候補。

## 1 反復の手順（成長 tick）

evidence（prompt に注入される）を読み、次の順で **1 つだけ** 選ぶ:

1. evidence が `TESTS-FAIL` / `PROBE-UNMEASURED` → それを直す（最小の差分）。
2. `physics.edn` の `:basis "estimate: ..."` を 1 つ、出典のある値（規格番号・メーカー仕様・法令の条番号と URL）に置き換える。
   出典が取れなければ置き換えない —— 推測で `estimate` を外さない。
3. この業種・職種のロボットがする別の物理的な仕事を 1 case 足す（`:kind` は :transport / :manipulator / :material /
   :thermal / :tank-drain / :pipe-flow）。README の premise と docs から根拠を取る。
4. governor が同じ solver で独立に再計算して、限界を超える action を止める純関数と test を足す（大きい変更。1〜3 が尽きてから）。

作業の仕方（これ以外の経路で main に入れない）:

```
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk branch physai-isco-5153 <slug>   # worktree を切る（path を印字）
# その worktree で編集 → kbb -M:physai-test → kbb -M:physics → git commit
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk land physai-isco-5153 <branch>   # 検証して merge
```

`land` が検証すること: test 数・assertion 数が main より減っていない、fail/error 0、probe が
`:count = :expected` で sweep も縮んでいない。通らなければ merge しない —— そのときは理由を報告して終える。

## 守ること

- **main に直接 push しない。force-push しない。rebase しない。** 着地は `land` だけ。
- **test を弱めて緑にしない**（assert を消す・sweep を減らす・限界を緩めて合格させる）。`land` は数の減少を拒否する。
- **数値を捏造しない。** 物理量は solver が出したものだけ。`:basis` は出典か `estimate:` のどちらかを必ず書く。
- **実機を動かさない。** これはシミュレーションと governor の repo。`:high` / `:safety-critical` な actuation は
  人の承認なしに commit されない設計を崩さない。
- この repo 以外（kotoba-lang/robotics の solver を含む）は編集しない。solver に足りないものは報告に書く。
- 1 反復で終える。報告は: 選んだ候補 / 変えたこと / test 数の前後 / probe の主要量の前後 / land の結果。誇張しない。

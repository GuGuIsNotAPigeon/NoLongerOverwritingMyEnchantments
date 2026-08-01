# No Longer Overwriting My Enchantments (NLOME)

不再刷过我的附魔书！保护村民交易中你收藏的附魔书，防止被交易刷新覆盖。

## 功能

- **收藏管理**：在 Mod 菜单配置界面中添加你想要的附魔（如经验修补 Mending）。
- **工作站保护**：村民贩卖你收藏的附魔书时，破坏其工作站不会立刻刷新掉这本附魔书，而会重设其职业或刷新交易（可配置次数阈值）。
- **交易刷新拦截**：拦截通过快捷键/按钮触发的村民交易刷新（如 Trade Cycling），只要村民还在出售收藏的附魔书，刷新会被阻止，并在交易界面以 toast 提示剩余次数；达到阈值后放行刷新。

可选：Trade Cycling（开启其交易拦截集成）。

## 支持版本

当前支持 Minecraft 1.21.11，后续将适配更多游戏版本。

## 构建

```sh
./gradlew build
```

产物位于 `build/libs/nlome-1.0.0.jar`。

## 许可

MIT License，见 [LICENSE](LICENSE)。

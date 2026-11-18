# OTMitems

Plugin Paper (1.21.x) gắn **skill** lên vũ khí thông qua **PersistentDataContainer (PDC)** và **CustomModelData**.

## Yêu cầu

- Paper 1.21.x (đã thử cấu hình với `1.21.4`)
- Java 21
- Maven 3.9+ (để build)

## Build

```bash
mvn clean package
```

File jar nằm tại `target/OTMitems-1.0.0.jar`. Bỏ jar vào thư mục `plugins/` rồi khởi động lại server.

## Lệnh và quyền

Tất cả lệnh cần quyền `ot.admin` (mặc định dành cho OP).

| Lệnh | Tác dụng |
|------|----------|
| `/ot <mã>` | Gắn skill có mã tương ứng vào vật phẩm đang cầm ở tay chính. Ví dụ: `/ot hz63dse` |
| `/ot set <mã>` | Giống trên (dạng viết tường minh) |
| `/ot del [mã]` | Xóa skill khỏi vật phẩm đang cầm. Nếu có `mã` thì chỉ xóa khi đúng skill đó |
| `/ot reload` | Nạp lại `config.yml` và khởi động lại thanh hồi chiêu |

Alias: `/otmitems`.

> Ghi chú về cú pháp: yêu cầu ghi `/ot hz63dse <mã>`. Plugin hiểu theo `/ot <mã>`, tức `hz63dse` là mã skill. Có thể dùng `/ot set hz63dse` nếu muốn viết tường minh. Mã skill không được trùng với các từ khóa `reload`, `del`, `set`.

Vật phẩm hợp lệ mặc định: kiếm (`*_SWORD`), rìu (`*_AXE`), `TRIDENT`, `BOW`, `CROSSBOW`, `MACE`. Có thể sửa trong `settings.extra-weapon-materials` hoặc bật `settings.allow-any-item`.

## Cách hoạt động

### Lưu trữ (persistency)

- Mã skill được lưu trong PDC của ItemStack (`otmitems:skill_code`). Dữ liệu này nằm trong NBT của vật phẩm nên vẫn còn khi:
  - vật phẩm bị vứt xuống đất,
  - vật phẩm được cất vào rương/kho,
  - người chơi thoát game rồi vào lại, hoặc vật phẩm được chuyển sang server khác.
- Khi **gắn skill lần đầu**, plugin sao lưu CustomModelData hiện tại vào PDC (`original_cmd_exists`, `original_cmd`).
- Mặc định (`settings.override-custom-model-data: false`) plugin **giữ nguyên CustomModelData** của vũ khí.
- Nếu bật `override-custom-model-data: true` và skill có `custom-model-data >= 0`, CustomModelData sẽ đổi sang giá trị đó. Khi xóa skill, giá trị gốc được khôi phục.

### Cooldown

- Thời điểm hồi chiêu kết thúc được lưu trong PDC của vật phẩm (`otmitems:skill_cooldown_until`). Cooldown đi theo vật phẩm, không phụ thuộc người cầm và không mất khi đổi item.
- Khi gắn skill mới, cooldown được reset.
- Chống spam click: click cách nhau dưới 100 ms bị bỏ qua. Thông báo "đang hồi chiêu" được gửi tối đa 1 lần/giây.

### Thanh hồi chiêu (Action Bar)

- Chỉ hiển thị khi người chơi **đang cầm đúng vũ khí có skill** và skill đang hồi chiêu.
- Thanh giảm dần từ 30 giây về 0. Khi đổi sang item khác hoặc hết hồi chiêu, thanh được xóa ngay.

### Skill `hz63dse` (Đóng Băng)

1. Cầm vũ khí đã gắn skill, nhấn **chuột phải** vào không khí hoặc vào thực thể.
2. Một viên đạn băng bay theo hướng nhìn, tối đa **15 block**, tốc độ 1 block/tick, để lại hạt tuyết trên đường.
3. Đạn nổ khi:
   - trúng thực thể sống (quái hoặc người chơi khác, không tính người bắn),
   - gặp khối không xuyên qua được,
   - hết tầm 15 block.
4. Tại điểm nổ, mọi thực thể sống trong bán kính `freeze-radius` (mặc định 2 block) bị **đóng băng 3 giây**:
   - Người chơi và thực thể: Slowness cấp 10, tức đứng yên hoàn toàn.
   - Mob: thêm tắt AI, khôi phục sau khi hết thời gian.
5. Hồi chiêu **30 giây** cho từng vũ khí.

Plugin dùng hiệu ứng đóng băng thay vì đặt khối băng. Nếu cần đặt khối băng thật, có thể mở rộng `FreezeManager` hoặc `IceBoltTask`.

## Cấu hình

File `src/main/resources/config.yml` (được tạo ở `plugins/OTMitems/config.yml` khi chạy lần đầu):

- `settings`: các tùy chọn chung (ghi đè CustomModelData, cho phép mọi vật phẩm, danh sách vật liệu vũ khí, thanh hồi chiêu).
- `messages`: tất cả thông báo. Hỗ trợ mã màu `&` và hex `&#RRGGBB`.
- `skills.<mã>`: định nghĩa từng skill (tên, tầm bắn, thời gian đóng băng, bán kính, sát thương, hồi chiêu, CustomModelData, thông báo riêng).

Thêm skill mới bằng cách thêm một khối mới trong `skills`, sau đó chạy `/ot reload`.

> Lưu ý: nếu đã có file `config.yml` từ phiên bản cũ thì file đó không được ghi đè tự động. Hãy xóa hoặc chỉnh tay để nhận khóa mới.

## Quyền

- `ot.admin`: dùng toàn bộ lệnh `/ot`.

## Cấu trúc mã nguồn

```
src/main/java/com/knmusic/otmitems/
├── OTMitems.java        # Main class, đăng ký lệnh/listener/task
├── OTCommand.java       # Lệnh /ot và tab completion
├── PluginConfig.java    # Đọc config.yml, thông báo, cờ cấu hình
├── SkillDefinition.java # Record mô tả một skill
├── SkillService.java    # Kích hoạt skill, kiểm tra cooldown, chống spam
├── IceBoltTask.java     # Đạn băng bay và điểm nổ
├── FreezeManager.java   # Đóng băng và khôi phục AI của mob
├── WeaponData.java      # Đọc/ghi PDC: skill, cooldown, CustomModelData gốc
├── CooldownBarTask.java # Thanh hồi chiêu trên Action Bar
├── WeaponListener.java  # Sự kiện chuột phải và thoát game
└── Text.java            # Tiện ích màu sắc và placeholder
```

## Giấy phép

Dự án cá nhân, chưa chọn giấy phép.

# Triển khai lên Kubernetes với Argo CD

Vòng CI/CD theo kiểu GitOps: **Git là nguồn sự thật duy nhất**. GitHub Actions không hề gọi
`kubectl` — nó chỉ build image và sửa một dòng trong `k8s/`. Argo CD chạy *bên trong* cụm, tự kéo
Git về và làm cho cụm khớp với Git.

Đó không phải chi tiết vụn vặt: cụm minikube nằm trên máy cá nhân, không có địa chỉ công khai, nên
GitHub có muốn đẩy vào cũng không với tới được. Kéo thay vì đẩy là cách duy nhất chạy được ở đây —
và cũng là cách các cụm thật làm, vì không phải mở cổng quản trị cụm ra Internet.

```
push develop
   │
   ├─ job test   ./mvnw test            (213 test, hỏng thì dừng tại đây)
   ├─ job build  docker build + push    ghcr.io/vanmo2003/booking_app:<sha>
   └─ job bump   kustomize edit set image → commit "[skip ci]"
                                   │
                                   ▼
                        Argo CD thấy Git đổi
                                   │
                                   ▼
                     kubectl apply vào namespace booking-dev
```

## Có gì trong thư mục này

| File | Việc nó làm |
|---|---|
| `base/mysql.yaml` | MySQL 8.4, một bản chạy, dữ liệu trên PVC 4Gi |
| `base/app.yaml` | Deployment backend, Service, PVC uploads, ConfigMap, Ingress |
| `base/kustomization.yaml` | Gộp hai file trên |
| `overlays/dev/kustomization.yaml` | Namespace `booking-dev` + **dòng `newTag` mà CI sửa** |
| `argocd-application.yaml` | Khai báo cho Argo CD: theo dõi repo nào, nhánh nào, thư mục nào |
| `create-secret.ps1` | Tạo Secret trong cụm từ `.env` — chạy tay, không qua Git |

## Vì sao khoá bí mật không nằm trong Git

Argo CD đồng bộ *mọi thứ* trong `k8s/`. Nếu để khoá JWT và khoá Gemini ở đó thì chúng nằm trong
lịch sử Git vĩnh viễn. Nên `booking-app-secrets` được tạo tay bằng `create-secret.ps1`, và các
manifest chỉ tham chiếu tới tên Secret.

Cách làm đúng chuẩn cho hệ thống thật là [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets)
hoặc SOPS: mã hoá khoá bằng khoá công khai của cụm rồi mới commit, chỉ cụm giải mã được. Với đồ án
thì tạo tay đủ dùng và thành thật hơn là commit khoá ra repo.

## Cài lần đầu

Yêu cầu: Docker Desktop đang chạy, `minikube`, `kubectl`, và `.env` đã điền.

```powershell
# 1. Dựng cụm
minikube start --driver=docker --cpus=4 --memory=6144 --disk-size=16g --addons=ingress

# 2. Cài Argo CD (server-side apply, vì CRD applicationsets vượt giới hạn annotation)
kubectl create namespace argocd
kubectl apply -n argocd --server-side=true --force-conflicts -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# 3. Tạo Secret của app từ .env
powershell -ExecutionPolicy Bypass -File .\k8s\create-secret.ps1

# 4. Cho Argo CD quyền đọc repo private (xem mục dưới), rồi:
kubectl apply -n argocd -f k8s/argocd-application.yaml
```

### Deploy key cho repo private

Argo CD cần đọc được `VanMo2003/booking_app`. Tạo một khoá SSH chỉ-đọc:

```powershell
ssh-keygen -t ed25519 -N '""' -C "argocd@minikube" -f argocd_deploy_key
```

Dán nội dung `argocd_deploy_key.pub` vào **GitHub → repo → Settings → Deploy keys → Add deploy key**.
Đặt tên "Argo CD minikube" và **không** tích "Allow write access" — Argo CD chỉ đọc.

Rồi nạp khoá riêng vào cụm:

```powershell
kubectl create secret generic booking-app-repo -n argocd --from-literal=type=git --from-literal=url=git@github.com:VanMo2003/booking_app.git --from-file=sshPrivateKey=argocd_deploy_key --dry-run=client -o yaml | kubectl apply -f -
kubectl label secret booking-app-repo -n argocd argocd.argoproj.io/secret-type=repository --overwrite
```

Xoá `argocd_deploy_key` trên máy sau khi nạp xong — cụm đã giữ bản sao rồi.

### Image trên GHCR

Package GHCR mặc định thừa hưởng chế độ riêng tư của repo. Sau lần build đầu tiên, vào
**github.com/users/VanMo2003/packages → booking_app → Package settings → Change visibility → Public**.
Làm vậy thì minikube kéo image không cần `imagePullSecret`.

Muốn giữ package private thì phải thêm Secret kéo image:

```powershell
kubectl create secret docker-registry ghcr -n booking-dev --docker-server=ghcr.io --docker-username=VanMo2003 --docker-password=<PAT co quyen read:packages>
```

rồi thêm `imagePullSecrets: [{name: ghcr}]` vào pod spec trong `base/app.yaml`.

## Mở ứng dụng

Ingress cấp host `booking.local`, nhưng máy bạn chưa biết tên đó trỏ đi đâu. Thêm vào
`C:\Windows\System32\drivers\etc\hosts` (cần quyền Administrator):

```
<IP cua minikube>  booking.local
```

Lấy IP bằng `minikube ip`. Trên Windows với driver Docker, IP đó nằm trong mạng nội bộ của Docker
nên trình duyệt không tới thẳng được — chạy thêm `minikube tunnel` trong một cửa sổ PowerShell có
quyền Administrator và để nguyên đó, lúc này dùng `127.0.0.1 booking.local` trong `hosts`.

Cách nhanh hơn khi chỉ muốn thử:

```powershell
kubectl port-forward -n booking-dev svc/booking-app 8090:80
curl http://localhost:8090/booking-app/api/v1/ai/status
```

## Giao diện Argo CD

```powershell
kubectl port-forward -n argocd svc/argocd-server 8081:443
```

Mở <https://localhost:8081> (bỏ qua cảnh báo chứng chỉ tự ký). Tài khoản `admin`, mật khẩu:

```powershell
kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath='{.data.password}' | ForEach-Object { [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($_)) }
```

Đổi mật khẩu ngay sau lần đăng nhập đầu, rồi xoá Secret đó:
`kubectl delete secret argocd-initial-admin-secret -n argocd`.

## Vì sao từng lựa chọn lại như vậy

**Tag theo SHA, không dùng `latest`.** Argo CD so Git với cụm. Một tag đổi nội dung ngầm thì Git
không đổi, Argo CD tưởng đã đồng bộ và không làm gì — image mới không bao giờ được triển khai.

**`replicas: 1` cho backend.** `ChatSessionRegistry` giữ WebSocket trong bộ nhớ tiến trình và
`AiRateLimiter` đếm lượt gọi AI cũng trong bộ nhớ. Hai bản chạy song song thì bản A không biết khách
đang online ở bản B (đẩy thông báo thừa) và hạn mức AI bị nhân đôi. Muốn tăng replicas phải chuyển
hai thứ đó sang Redis trước.

**`strategy: Recreate`.** Cả hai PVC đều là `ReadWriteOnce`. RollingUpdate dựng pod mới trước khi
pod cũ chết, hai pod cùng xin một PVC — pod mới kẹt `Pending` vĩnh viễn.

**`startupProbe` 5 phút.** App mất 60–90 giây để lên (Spring + Hibernate + chờ MySQL). Không có
startupProbe thì `livenessProbe` tính ngay từ giây đầu và kubelet giết pod giữa lúc đang khởi động,
lặp `CrashLoopBackOff` mãi.

**`paths-ignore: k8s/**` trong workflow.** Job `bump` tự commit vào `k8s/`. Không lọc thì commit đó
kích hoạt lại workflow, build lại, commit lại — vòng lặp vô tận. `[skip ci]` trong thông điệp commit
là lớp chặn thứ hai.

## Lệnh hay dùng

```powershell
kubectl get pods -n booking-dev
kubectl logs -n booking-dev deploy/booking-app -f
kubectl describe pod -n booking-dev -l app=booking-app
minikube stop          # tắt cụm, giữ nguyên dữ liệu
minikube delete        # xoá sạch cụm, mất cả PVC
```

package api;


import java.util.Map;
import dict.KeyError;
import dict.Dictionary;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import printer.PrintJob;
import printer.LabelPrinter;
import printer.ZebraLabelPrinter;
import org.json.simple.JSONObject;
import com.zebra.sdk.comm.TcpConnection;
import org.json.simple.parser.JSONParser;
import com.zebra.sdk.comm.ConnectionException;

import static spark.Spark.*;
import com.zebra.sdk.printer.discovery.*;


public class Server {

    private int port = 9100;
    private final ReentrantLock queueLock;
    private final Dictionary printerIndex = new Dictionary();
    private final Dictionary printers = new Dictionary();
    public Queue<PrintJob> printQueue;

    // Constructors
    public Server(ReentrantLock queueLock, Queue<PrintJob> printQueue) throws ConnectionException, DiscoveryException {
        this.queueLock = queueLock;
        this.discoverLocalPrinters();
        this.discoverNetworkPrinters();
        this.printQueue = printQueue;
    }

    public Server(Queue<PrintJob> printQueue, int port, ReentrantLock lock) throws ConnectionException, DiscoveryException {
        this.port = port;
        this.discoverLocalPrinters();
        this.discoverNetworkPrinters();
        this.printQueue = printQueue;
        this.queueLock = lock;
    }

    /***
     * Starts the API server and declares all the routes
     */
    public void startServer() {
        port(this.port);

        // Set up cors
        options("/*", (request, response) -> {

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        after((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "GET, POST");
            response.header("Access-Control-Allow-Headers", "Content-Type");
        });

        // Get the status of the print server
        get("/status.json", (request, response) -> {
            response.type("application/json");
            response.body("{\"status\": \"ready\"}");
            return response.body();
        });

        // Get the list of available printers
        get("/printers.json", (request, response) -> {
            this.discoverLocalPrinters();
            this.discoverNetworkPrinters();
            response.type("application/json");
            response.body(this.printerIndex.toJSON());
            return response.body();
        });

        // test printing something
        path("/print", () -> {
            after("/*", (req, res) -> res.header("content-type", "application/json"));

            // test path
            post("/test", (request, response) -> {
                ZebraLabelPrinter p = (ZebraLabelPrinter) this.printers.get(0);
                p.print("^XA^FO20,20^A0N,25,25^FDThe printer is working successfully.^FS^XZ");
                return makeJSONResponse("success", "Test page printing...");
            });

            post("/testImage", (request, response) -> {
               ZebraLabelPrinter pp = (ZebraLabelPrinter) this.printers.get(0);
                pp.printImage("iVBORw0KGgoAAAANSUhEUgAAAyAAAASwAQAAAAAryhMIAAAgoUlEQVR42u3d3Y/bWJYYcHK4ECdYT3Ea8xAVUqs7yQAzj3GlgYyMUcQsBsi87eQviFXwwHoIElfFwJRqLYsUKijuQ8PafatCaqXNP5Dep/ZsvLZYq4G5nXWsPMWY8dqihgtpg4ZbFArdumxRPDmkVFUq14f1RY675hIqSZYl/XR57znnXn1yEMHGveeIzBCG/E4h6nWJFuRS2IhoJuZCXLnhH6fJxUjl0DIekLPIoeDQu1CzXRmIVDOVtxGt+olISU/9Luk3XvNq5t6rzmXIYf05kc78Z1tw7BtQNHMEEmLxLHKv+km/3/6s34FepwzVzsPPepcipafnIXLXWobdpzdKXuLWbvvt3eUNqp8M+r0T5MU7kEqLXDvzn3W52d6HVP3DurdymLoA6fsIdP4DIo3OpUi70vrpWeS58ikgAh/W3f3DVP0MAgHyqt/pT4Co62Kl9Uc/PYv85FewAq4n193Kofu3Z0aXcoQAvH6NSKXRuQyRz0XKguOmFZcqdQcR/UzHV9R/3fcRk8CfvxbUjFypX7a76ucjq45bUByKV6i0nafy20gbh3C//8RvyeOgJdXGZYhRaf3s7KOoOA6kla6nkMOK0YUzSL/6CfT7qo/c85GGUr204+tPL0DKigXp5A8qJessMggQvyUejSHyGpRLI77+/EKkDencd8VK+wziBcihcq8DA/Bb8sqDS+PEeHAhUsdBbIpy/QKk5yP9AHk5uBwxExvPznb8ltMuK5X6DTyLJ/I5aQX6HR/pBcjD/qWIQwvnIR86VkKpPv0RnoXq0/MSJNAEPDL/mS29BjWz/k1x+qJVxwSZAs5cl2Bf4M4myIVUxrrm0Dxwth0gdjjl1x8K7qIqo+l4Cnhnx7ifd51FIfqNOZGad041egsp3Tj/Pw9HfxchHdv8/c6TJQ4eu8+zPUouRSp7s02JXnQK/c7jXgcKg897vX4nHKTxpP/iEd55of9Fv9fvXY44Sz/bSvws/YMwkRqNZZJLmRVxSuRR1eu/eAid6iSImXt6K44HY3pk8GgTOvDXAdK+FKE3Wtk9PLRnRYo+QqXLI355NuR+gGx3IDVBS2BvNoRH5K82Jx1de7O25MngfsiI5zUQuTchUr/RupV8nt01ZkaCiO+9I0G2ME6y++K0SKfgeQOrM8xd70grttPCiM+Wp4z4Aax7uEmdau01kew4eVfRajuj1B7mwtRHHCMChEqhLkwVROptl8yCNDr9JoW8PKqNXZvm5XMRvLRqYI90pkNqA1v2kWdt5d7ff5590chtfvW8dfdG5dxgxCbIPlKfDnn8Va8Olfrh686TzTdf9l40Nh992biz+ahxLoK0XJseKXyFpffmx4fVm97mZ4P+i4/vvPq82vj4RedC5NOZkJ7XuPNZ9eZgAqRMHkiftjVre7qIh97ml9gnR8jDj+94ry9GUtI3pGdtQZ8e6SHysvrv+/f+1x/0H/28B+n/fhGi5p/elZ+1V/frMyGPqj/dzC2/QQRXf+ULkRstRX5+e6syLdJ5OERuPrz7d18g4inp6s2LdldclBDha9Mi5g995B7e8eZnX/Tv/ReoXoLsippc/z7frE+b6oOWcNWbf/FPEfnjr+D164uRPThERHGnHcKNF20/4qs3q5sB0sEhfDHSuivXd7b2p0c6PvK6U/UjPkAujvg4jq7602lHFzxuvGiM5a77g96jLy/MXWpKEqW6IRzUpkyQVTMdZGEb8mmyNBjQza+aF2bhAimV6hjxFnte+KogQVd3VADv/GnCcOlAC0RzFYKH4WXtySJe/JVbb/FF6D1qVBudan/g9eFl/yLETknCGKJMiDg+Ak+UzotOo1qvDvpuH351EWIfpuofujLBg//PtDwhMvi1+6IFVaXT6HSUj6te76s+/PpCpL3X2nLTZPRcuzwp4v36qxf/F6pV8JE7itf7sncx8g/zIQ8R+RI6AtzxbPnlvcuRRCqdWPrxn4I82VobkZd//DJoSbXhQOcGVLzDyoXIswApr6yUV2K8qqTz4qTIz19+wiOSJojkEenVXz66ANkPkP3dZ/t7rdWPFC5nTIH4LVHqiHiIQOddyN6z/crTrbIiOO3pELzvE+TlBUhqhLj75RgiqcmR//Hq54gMwO94Dzv+EoT7gY98uud+uv99H9maEHGHiNzv4RDGpV3P63VefvZOBE/LSnpSZDBElF6A3PeRxoWI2x4iz0bIjQmR/svXr7+EJ9DpNxrVzl8N+l6/8vLwIiRIK5/uIuJ3vJyacHSJPsIXe+aKjAmyhgnSlS9MkG6QIK39FWs3FiAxZfH1xA1SvVVOWfu3faT8feX9K1oMYQhDGMIQhkSG0Pzopb5D0NqHUBKaXkpaNGKmRshdjw8QPQTEGD2F4CluDhFJfpZPLXx31UvHiBsg5RCQUv0IoW720Psjpbx1Y6J518M+2Pa3BkX7mvBOpOL+bX7rT/8hfe07jucjP/G2bpBJZpCIdDbdwZPNQX8CRE/eUDMrv4dIRnCfC97d5AQd7w0QaTwc3K9uDgYTIE9X9z66tdy66yPO81W3nTSmQR5OhNQ/3CtnK0/v+rvLqTu0PcHadIT0Bo8nRpxythwLkMO6061PhDh+n0yMPAuQ/e8HiC04h7o0CSIgcmh/61X1L7/17o4vDZG91l3XDZD2ZIjfErc3eFV96IrvDsb9oE8CBIPx+VYdbkzaJ7A5eIO7q/futLJSX/WRp3eDtPL8wwYk65MiDwPkcIIEWU/eKGd3Y98JEmRdUJXUFHHyRfWHEwSjna+ntrDjs98JUn1dU5U0mQaZJBhnLFqjtDJwnvRCRMS/jEvDBPkt791xUhdnQaZL9ZXnfATIs60oEPfKIBBFx5MIkLoQBXIQBUKjQK7KEC59FAFSL0eQVvREBAnSVrSrsJyzuTA3PkIkgo0hUyKq2AQvGI5dAFc6mm82FoyoyhAxAahwNHOuLBRxISEfIzZ/9Lo/WSziyeQYadphIUpliOgKWDQkxFVKnqwZXfilj6g0leuahe+KC0ag7qX5WjNAQLVXcl09zYWApDjLgh0Zu0U1r61vqInfCwO5jsgDGWzZsh5cv6UlCFk8ksytBQgVLePp9Vv1hBwG4m4YgI/eExD5V7e8UBE4sPTYv/ieGQJSGiIc3rEdIPrCEQzGctLNaR4n+cHoI9uLR2SSQKQ0Qgz4wQ9KC0cwQSaSOUTkEjQRqYtiCIiqXEtddyVXMcC2LUsTxQeLR3RQ85xLAoQ3TUkU1YS82Ij3N9XjhwgVTFuSND0MpAmaRyimR0+0qSxpzQeytnAEN0+G0LZjxL0yyNsfWvv6Ik02F2YIQxjCEIYwhCEMCW8bRIFYUSAkAoRWI0CMKJDqlWlJJH0SyeiKJk4iifjGVUE4PgKkHkVLDqNABlEg3s0oRlclAuTJ4L1ByIIQCoab5lICiME7Mqpj17H9jx92F4TYSyeIPHYdnSwKsUFDJCmAUIS3no5UpUUhJoiW5ubgHETy33y3EET3RMug5yJEmxNRjxKk6ooGUOojrhzPSTuy9MuUst0VKJB64nsHXSrPPxfedQ0DbCrBd/7Mlr6dlHAM6N+WeZu3sVUJQd2wZx/H1SPkFwPjl3LXFhExRS6OY4DXuQRnr3Z9ROQ2TGn2PoGTIayTpi3ABx80taVEOplcq+0kknY3QMjKqlVaDCJZXQuRWo0kUqnrtlFayW/YAZJObxjGQpCaZuEg+uADw0KksEoDhFoBIncXhOD2NsKdILr4dUOWV08h7o89uT4nchyMQ6T7FqJ03Z8AMYKOnz0StbGWtI0SIvHV8dEVIBoiq0Z9ZoQqJ4hVKlELkaZGgjgxSonkhidigkzIGCdzvPq0fYKYUokKiJgiSfgRjwjX9SRM9Ql5roiH8aJFSq6IiC2RhPSNlGCUSIDoJCGrXXt2xCRj5Vcu4V2ubLgyIX4WRmS7CxKW34Q8VxYuX7Q+Oa4oc04i/E25dz7iHiHeIpD7FyD5KBBOGc2H5PkR+QLEO7p0EUi5f0HHF4+QBUxTD3oRzIUPxAiQv74XAVIYMOR9QyLp+FoUQ7gWye6KpOPvR4Aoj6NAXkWBvIniyecvonhSzRkh9bYqqKJOOdkCV9FFk/cnv7jyKgL1ZN3liCtZtKuTenv6lsApRE36CJV1foTwHqfQgsylOEIFHxFnQJ6cRrj4WwjncjItpLkVjtj8vIjHc/5bbYlkgI8YtgU+0qX5lEzT6QSx/DcS067xNzw3LyKXfMQmBh0hbq5AaDolS5b/tkJEfjMHgrvrBOmOI0BoIqWUjhBrnj7RagECbyHeKpRoIomLF/AZRLTa7IhQ9BEpQJolozuOPAgQc4gIxdnXjB/uuYoqCQFilXB0CW8jtmxyXaP24fRfQX28Znz40kdUZYQI48jTAKEiIrr48OXsa0acYyNiyj5iGKdbMkQ8wUdmWm9tjyElcMkQOY6TAPlNgMBBkFbmWTMOEQiCUReHo4uSU0gQjDNlyMY4MooTVRhHJGonQTuKkzCQIBjtsWCc6blB/hRCAqRknU4rNEgrzZmR+qmWgBT0CemeTpA0SJD2zH1yOI5gqsfRZfKk66d6XThK9W6a8xF+VmRwGlHlESKMkKBouXJQZYRZkbEXaXzExIg38bH75TdAxFH5Dd5IPPPoiuJFGlW4Mi/LMmSqzboqb/OxpSgm3O0IkEY/ivXJgCG/k4jKX5WIb0TxLNGTKILx/uDKIade2e3g2s2W4cCTTLINs78u55QsO63AY+c8pA6qjMsdLi+ahJ8DOSxZpiRD+fA8pAQcUXHc5QQ9wYE2D6KLMpjSuYiXIJrmrSQtw0vC7C+VWiVckRMoXIDIklTz0vMi7RIyFcif7K5aV6O3vt0F6WlC8OQKsbz0atdw84XZEcNHSrB10vFcV7AF7t+6wi+XBFfx5K6P6Pl5Ebzne8dD2OPWcV3NcTlej42QlY2umsrnfzIPsn0aia83re2leG6tppUCBDhEVuZBSji6MAJOEDe12q3VEql12zBGSLHbLa3kc8Y8iIlrt3/SP0YK2M1WIrVBjxFsCXyU35gLobgKtduXICsLQEA0TorWOYg/uhC5NR9SuwQpy80RkuHnQUrWhYgnl4bBWFowMj66jtNK6UHSnAPRatopxItvdK3tRCq35iPHCdJ//nF2pKbVxvtEjeGANQVEeMPYPpXq50B0URfrY4jIdbv29xKpvOAjQdFSXdGU5ylahimZUmkcqXW79FYipUiGUTsuv7YyT/k1qExJ6e3JnQUL3YzjSjWG1BeMtIdPrJxGjNpikeAxk7cQXVwsEnxzi/wWMs9beSZGKFks4n9x7fA3MtmnmxjCkEkRR5cNmgIQqXyASQdTPF38N2IcImInFE+wiYoIFqswEFXWzCXF5U2JU6CGZddePIJzfNHakem6tbMiw7Z2Lbmx+D5plzzRMGRKjadpRHC+FQKCs0ZEyAgRQkJU1wCD+G+69BDByd76+Xm/17TtlZQozYRw9w3QfETzZO97iGTO3I+ejPHQe93ZdF55m7MgJU42XJwj2a7k+ohEM2dGV+ORh51Xvfnw8JXbmwlJIIJzpKZLqOLe8ohtnZlvNV4i8gJuPuy9ufzbhC9EgAQtOXBxQkctl5hnJ2EB8hCUj3ufzY6Ahqt50GxARG6ej1R7oPzF5j/+weyIUQINjKa9alGldkFLeh5kf/j//vdciIQzPPs6Z6N2FvlFTICe7d35b3/XOpwJIcTwFyY43d5GxP/MyxlEvxe0pAB//y97vz8H4uKxCDaHE/qzSCNA4D40Or3ZdtcD2bC0kitbGgahVfPI2RcuGz8KkHuzI6qMS5aKy2Gql8HSl7j184OxV93sNF72ZgpGQ1cMmxDK2QkfUa9xOen80VW903n48s1siOmvixCh6RGSJ+cjrxu9H/358/ZMCByvJS7cGv8Yw2Bs6sl8YdovweNOFkVTfBHqbOXXP/JCRoIIHq4lvuZI8GseLoSLsAn3+4nUmrYXKnLnMw8eP2sVsDRBTqg1GqDbS43/Iy4Uyb4ZQOF1J48ZqlCtPkaksfl5vaEsFPmPPlK9+T8ReVKtFnzk4ZcvQkC86s1PQkX+0+f9KiJPEHnceAJD5OXiERgMkX8TGvKfEXECRL+PiE78jn+1aKSzCf1hS37UCVqi24lFI3fubOIcIUB+IQ13V+/16xCQjo9IjUedJ16jITV6zxeOfPwxNIYtedgbDuHe542bi0eqrzuFceTLhSP1j0EJcldjszdMK4hUFox0GlUuyMJ6p1+tBchXjQX3CauMDGHI7xIi0OkyiCupmXVxSuR+rzoVMjisdqrKlEih05gO6Vc7jdCRwSxI4830yP2pkU74Lbk/NaLembrjhelbYirTIsoMuwveT2TaIexEgfRnQaZNK71ZEHp/KgQT5B2YNhhZ0ZoYCffHiHATo0JYxzOEIQxhCEPeR4Ry/i8nAXiiyXHyFURU7nbWjGXVuLibiSltxylnHWg7dM1xKo5XUIrFckFxWpm46JjxWGF5R4QizTbbRTW+DJV2y79A5St77auGkOFuQ0QZG10MuRihEtBE2IgpgBnzEU/0/xkOovOgfyNABHscuZ1Q6G2eFuJ8iy/iwVxuV+iWx+8sqwWypyptM5bJLKuxXIHHe9slaux2gVujyzSOFzhvISqHh2B3CaYcFsJxio/gSdGXQkA4rsCbMmcHiKqHhoi2/yZHH9F1CA9JC26ABJ+VCAXJSxglXkSIEnztUEijy+W4Y0QcQ1prKnHKipnAQ3ZnSyVmtswtV9o0Fs+aRGu2+DYVWznudiyRWXYQqWhNk9f2yhniLWuqeBFCGfJuRMLMeE7HLx5xA+TUEF4o4uE+EsJHKPhpBZFTaWWxCG8TP0EicjpBZncJVXaUViLbBK1YbDZpIUuzO6K33PbITi6jBEXLI1pZNLPOjrijNDEit9pe1ottUfJ2qpeHqZ7wZmiIysn6EBFsOTwEM6OPJE6X34UiOKZswUdWTk8kFopgnLtSgEgYMyEgFyyCrhhymzdzGX43geHGF8vLMKpRax4Plb2djFIx+VYurrRihTVTdNQtKGcSCbIbI80yv0uKUSOHw7cs4w1hP3SEUyJA8vLRb5GHiCjhIrZzTRY9pSLu1+vin8lyKIie/GZK8JR9Yb+9LxRDQWyhnrT2u4h0EemWysoJwil7LUUt5ESnUqHLzWZll3e0luhx2WJbTWSybVUs7vmTOsXMYrSW+bbmmFs7xEzEMnzzFIJn9xxP2XP28bQSCnJ4iGd3eU/Z5fcPk3zlGXytkT0cwjdg//AGVJrhIPUb7f2Bl08N9ml+EBaiI8J7XJ5HhK8choPYTrsi4NgX9l0QwkHe3qyTyV2OFJ2mGo+RlqL5TxHc9mtUmVcLsUJB3FMzpE0LiS2tWfFiMaVdwUfSppncFuy1vVgho1yCtKNAtCiQUhQIMOQ9Rkycw+X4otbiHcePxthaU92ifhVbLrZhJ0O0ViZGKL8bx9DbVVSl3cosF4v4aPjybW7SeRdDGPL1Rlq5BBffcszY8i4Pu7mceFyjuNu8VvGWsarFYygkCllNxVVRjsvw5URuDR8PjwWMITMj7vgbShSGvNdIs1JUcwWxWc5WcB3mtL1s0YxvlZW2x6vZdqVJtzBY48tl0vTiYtGvZlkzW1TjhYKyw1daPEMY8juBlPkmTcQThS2TOMUKLax5GWWvQsWWWObRX3NMcaeQy2XEnWU1Ie5wy8WTpxPKYvG3gBx/kNtkCEMiQlqFGBfHRX85wRW2drlMYa1SUeOJTKwg7vIVTY0lbmcKXJzfJa1YVhU1UyyvUb69W+C2WrECl/1tIMFPsekiQxgSGdKsOFrwEkyikBD3aIG06ZpDlcpuIU60SpHGcRrXIrSg7BaUVhZ2lLbWdszcMsahl8UydzHiRoEAiQKxokAGV6Yl5KqMLpcfe7KAFuLLoLX4dqVd1MxC1suQ3cQalDNrqqgue7kszYi7MREXQxm8aAsngLj2obc5jOJsW7tkd/WjQAYMmQJxhShaAlcQae/hpI6umQluWcNyla00m7siLfCVnS3HJLt8kfrvMltWs46XifHFSrFprjllbgvXP14ia4oXIyYfAeJE0RIvCgSuDELF8JDhZ80Q6fUjQDpjNb4VJ46j5mIF0qzsimphza9YpNjcWW5rUGm3sWSpufia43hrNCaqmVgis7XD+3WulVg7/eaYq4qM9Ul4yNjoCnF3aVEg9yJAPh9Ei5jLXiFHKlp5uQhtdatYbDpFx9EoXxZbYnMnozhYzvAxKNoOcYptDEqnFVPUOFe4HcuapP1bRqz74SEns5W7ESBjcRIe0utHgHQGVwxZ9HZun4SLUDECJKSPGv4WEJOPAHGiaIkHrOPftziJJOIjyV1XB7k6WZhGsrvuR9Hxj6NAXkWBvImiT76Iomg5bAi/Cxl7MTO8ITyOvIoCeRMBEt4QHkNCG8Lj7/YILdWPI6EVrXEktPJ7NZFI+iSS0RXahDtyJLTl3DjiRNESD1jHTxMnUfzQc0gTidNbSBOJt5BXUSBvouiTLyJAQppInN7YEGZD+Gs+hE0hiq+gGzBkms1i3z3IEIYwhCEMYQhDGMIQhjCEIQxhCEMYwhCGMIQhDGEIQxjCEIYw5H1F2A9zstHFEIYwhCEMYQhDGMIQhjCEIQxhyG8V4ThPAqqY/k9Bu5xsKrYCIEIR1OAXEOdCbJ7L0UPZR6iAiCoDFRBRZZ1A8BWbqmiK8yPJ7hCxebAVjuBFiHBExYZxHqeooi7Mu7tIIj5CTA7sAieByVEuzUmcCDbnc4tASNzyETgwbTALEiImVbwlSdTAtnMpIsH2wQIQo6X4yAF2e55ocGDnFJfEpBo0aa6AiNZcAFJS/eGzXaOKnpc1qFFE5B2CiEMXhCTiUoCINddHDDhEBBCxwAIKi0G4pBjsLkRkNS/XAGjuxwA7pAk1sEH6prIIhAaIJyHyzRHCjSEcWQASp7f83TWG2OucAjEfEW1Y4qRabW5kJWcFCKm56fhpROcDZBHBmGz6u8slBxQRsg1g2jkZA+UAdA4Raf604iPdZ3jqyqadShYkfNS668quJBYRWQepoi0AIfEAobLJIcIhUsR9RwnHg24jArWFpBUaIIrNJXOKj/CI2ETl4YBe95FFpJU/GSHUR1QBPDwQU9Z5qLkBIpqLKlqYFK9dx+xVA1dERFfstSNEsheF4B251zHR14JftiY6QDNMBKutR4qhIeuIGGDLjkcE/zNWlpsEIi8UcSgiFQ1MpemmRahjqneThRVZWxxiYolSsHz4iOkjQdFChGiLG11NzCZA/fKrA1ZdCUblNyGJ88fJCWJzOI6xVzCZcHlOVP2JRN6fUqj8AhHeRwioXA7nK+LRlIgj86eVE+RocqdyeR6R48mdPH+CPEGOpqkqpwiIHE1TTWXuaSpbOjCEIQxhCEMYwhCGMIQhDGEIQxjCEIYwhCEMYQhD3n+EEjjwX1s4ADgIDbEl4BREOCVExBQ9TjY5PIJiaIguuBwxOTyKBuFDQ4pFuiKZgEdeeMj2AU37SDp0RPcRN0zEViSMFCVsRDyQ8IiGjOiCj/xhaMjBto/weGT/YYhxgvev+ogZZlrxES5cxPb7fFv3ez/klrRNPKodhNsnYK8roxe/Q8pdR4h4EG6chI2I5giJSVEgHBbHMBEjdIQqkmjnlKVwkbQkmLl0mIigBwgNFQlqfFHHoxDnXSqPE5XgKEzEnxLpISO64M8g/aNQp6n+XFgMdy4czOpdEu6snq20GMIQhjCEIQxhCEMYwhCGMIQhDGEIQxgyGVIDyVa2Af9c4skugTaeMcGVKYhggkjlZhuo4p+hHFXcAoEDV7AVDS/cpud8lO88RHUFU+bBTHNU8mQqQZ2nkq5QiXoC6AXBJupdsImHZyhnEzcvHt+E8FRU5UkQLi/oCQ7wzxZd2RbhOZ5R8Yzt8qCmeFNCxJRcPEM5/MsJwOWCm5iJ4JoTIN5KrqldSyrGSnLDoHK3Bg+SXeOB3DRMdwPPr9e0lVWoldwNqwTdWslet7yVJOBNwFhKdmGJTIKkc93SR3lE8j5iWVAOEKutBwg1jMQNMOoB0jTq9sahl04C3gQMfDhAJkNW/Vso+HfLsn1kBc8QYlkqpf4n0I2nOAQMg6IGlmGYG90RopTc/MbkCJTcIwROkF0fIRTGkFqAgJeUYIisk0kRaYgkLes3Mo7oI+QXR8g/P0YMw/ilj1wXfeRP8vmMtDRRx6fxFhJVSg84H/FELzVCaIDYsMO5ygjRhoh7XYBreUVN5TOTjq7rgod3VSoJlvVU9qQR0qwNkSYscXmlpgVIqaZ9tN4EuuGPMEXFNhv6RMHIXW/6ER605MFJS2xhiBx4SyspxRSHiCmu5Pzv6ui6CU7RsDXWZMGornZduYlI0kegNkLoCKl5iXRKsYcIscWVPCKAN+H8saL4N5kA+ZvVLlVqEIyuBI6uEeJ+b4hobhoR+pMhQv9dqvABNAFv8l+HyESjC7zVrg3aEOGkYwRGHS/RAiIw7HgCTznSxRGIN3GnRDDXHiHWW0hCss8i2128ySi0JouT1Fb3wJMgSCsBkh6mFYP6aSWFYwIRI8hdnmw84D6wQOx2D9wc1P1unAxZyVk1j0CQID1MkCOkOUJwTKzgEB4heLpuYVhZNRcTZAKvOVGCBI4K+hKnBKnewwzvpYepXqJBqudMkctj6fBTvSfjaU4ELok34YfVYbIhzLuieo1TsAqNihYJihaWKL9o5ThdVCmYsl+0EJHdnORxKVFdEiC4ia5MWH595KT8kvHySxFp0lH5xcfgl1+PS0uqFJRf/5ozTSTO7GSgi5+tVM5c4iweKZ25pLt4pH7mksOvKdKe4BI24f66IuYGFbq64CWJmnQV3S3IxJaFog5F3ZV0PGOuy4ItcxvULMjukmgWRMG/AiXBFfCfwRWSpEjdguhKVHggMoQhDGEIQxjCEIYwhCEMYQhDGMIQhjCEIQxhCEMYwhCGMIQhDGEIQxjCEIYwhCEMYQhDGMIQhjCEIQxhCEMYwhCGMIQhDGEIQxjCEIYwhCEMYQhDGMIQhjCEIQxhCEMYwhCGMIQhDGEIQ353Efa1gAxhCEMYwhCGMIQhDGEIQxjCEIYwhCEMYQhDGMIQhjCEIQxhSLjb/wfaE7qLhQ4sWwAAAABJRU5ErkJggg==");
                return "yah mon";
            });

            // base64 content with no splitting content
            post("/base64LabelCode", (request, response) -> {
                JSONParser parser = new JSONParser();
                JSONObject json;

                // Load up our JSON
                try {
                    json = (JSONObject) parser.parse(request.body());
                } catch (Exception ignored) {
                    return makeJSONResponse("failure", "Invalid JSON request body. Please try again, homie.");
                }

                // Get data from the request
                String printerAddress = (String) json.get("printer");
                String base64content = (String) json.get("printerCode");
                String printJobName = (String) json.get("jobName");

                System.out.println("Received a print request from " + request.ip() + " for " + printJobName);

                // add the print job to the queue
                PrintJob printJob = new PrintJob((LabelPrinter) this.printers.get(printerAddress, "->"), base64content, "base64", printJobName);
                this.queueLock.lock();
                this.printQueue.add(printJob);
                this.queueLock.unlock();

                return makeJSONResponse("success", "Successfully queued label code");
            });

//            F printing images, we ain't doing that no more
//            post("/image", (request, response) -> {
//                JSONParser parser = new JSONParser();
//                JSONObject json;
//                try {
//                    json = (JSONObject) parser.parse(request.body());
//                } catch (Exception ignored) {
//                    return makeJSONResponse("failure", "Invalid JSON request body. Please try again, homie.");
//                }
//                String printerAddress = (String) json.get("printer");
//                String base64Image = (String) json.get("imageContent");
//                String printJobName = (String) json.get("jobName");
//                PrintJob printJob = new PrintJob((ZebraLabelPrinter) this.printers.get(printerAddress, "->"), base64Image, "image", printJobName);
//                this.printQueue.add(printJob);
//                return makeJSONResponse("success", "Successfully queued image to print");
//            });
        });

    }

    /***
     * Discover Zebra printers connected to the server via USB
     * @throws ConnectionException If there is an error discovering printers
     */
    private void discoverLocalPrinters() throws ConnectionException {
        int localPrinterCount = 0;
        for (DiscoveredUsbPrinter printer : UsbDiscoverer.getZebraUsbPrinters(new ZebraPrinterFilter())) {
            Dictionary printerInfo = new Dictionary();
            String name = printer.address.split("#model_")[1];
            try {
                if (printerIndex.get(printer.address, "->") == null) {
                    printerInfo.set("address", printer.address);
                    printerInfo.set("name", name);
                    printerInfo.set("type", "local");
                    this.printerIndex.set(printer.address, printerInfo);
                    this.printers.set(printer.address, new ZebraLabelPrinter(printer.getConnection()));
                }
            } catch (KeyError ignored) {
                // Because in this case you will NEVER get a KeyError
            }
            localPrinterCount++;
        }
        System.out.println("Discovered " + localPrinterCount + " USB printers");
    }

    /***
     * Discover network label printers available to the server
     * @throws DiscoveryException If something goes wrong when discovering network printers.
     */
    private void discoverNetworkPrinters() throws DiscoveryException {

        DiscoveryHandler networkDiscoveryHandler = new DiscoveryHandler() {
            private int discoveredPrinters = 0;

            public void foundPrinter(DiscoveredPrinter printer) {
                Dictionary printerInfo = new Dictionary();
                try {
                    Map<String, String> printerProperties = printer.getDiscoveryDataMap();
                    if (printerIndex.get(printer.address, "->") == null) {
                        // This printer was not here before, so add it
                        printerInfo.set("address", printer.address);
                        printerInfo.set("name", printerProperties.getOrDefault("SYSTEM_NAME", "Network") + " (" + printer.address + ")");
                        printerInfo.set("type", "network");
                        // We need to use a different separator than "." because IP addresses contain dots.
                        printerIndex.set(printer.address, printerInfo, "->");
                        printers.set(printer.address, new ZebraLabelPrinter(new TcpConnection(printer.address, TcpConnection.DEFAULT_ZPL_TCP_PORT)), "->");
                    }
                } catch (KeyError ignored) {} // We should not get a KeyError from this operation

                discoveredPrinters++;
            }

            public void discoveryFinished() {
                System.out.println("Discovered " + discoveredPrinters + " network printers.");
            }

            public void discoveryError(String message) {
                System.err.println("An error occurred during network printer discovery : " + message);
            }
        };

        NetworkDiscoverer.findPrinters(networkDiscoveryHandler);

    }

    private String makeJSONResponse(String message, String details) {
        Dictionary response = new Dictionary();
        try {
            response.set("message", message);
            response.set("details", details);
        } catch (KeyError ignored) {
            // This won't happen in this method, currently
        }
        return response.toJSON();
    }
}

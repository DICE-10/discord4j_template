import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.rest.entity.RestChannel;
import reactor.core.publisher.Mono;

public class Main {
    private String Token = "";
    private static Long JoinChannelId = 0L;//チャンネルID (742027434089644135 ←こんな感じで末尾に「L」)
    private static Long SendChannnelId = 0L;
    public static void main(String[] args){
        DiscordClient client = DiscordClient.create("Token");

        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) -> {
            // ReadyEvent
            Mono<Void> printOnLogin = gateway.on(ReadyEvent.class, event ->
                            Mono.fromRunnable(() -> {
                                final User self = event.getSelf();
                                if(JoinChannelId != 0){
                                    RestChannel sendChannel = client.getChannelById(Snowflake.of(JoinChannelId));
                                    sendChannel.createMessage(String.format("%sが起動しました。",self.getUsername())).block();
                                }
                                System.out.printf("Ready %s#%s%n", self.getUsername(), self.getDiscriminator());
                            }))
                    .then();

            // MemberJoinEvent
            Mono<Void> onJoin = gateway.on(MemberJoinEvent.class,event -> {
                Member member = event.getMember();
                RestChannel joinChannel = client.getChannelById(Snowflake.of(JoinChannelId));
                if(!member.isBot()){
                    joinChannel.createMessage(String.format("ようこそ！%sさん！",member.getDisplayName())).block();
                }
                return Mono.empty();
            }).then();

            // MessageCreateEvent
            Mono<Void> onMessage = gateway.on(MessageCreateEvent.class, event -> {
                Message message = event.getMessage();
                if(SendChannnelId == message.getChannelId().asLong()){
                    RestChannel sendChannel = client.getChannelById(Snowflake.of(SendChannnelId));
                    if(!message.getAuthorAsMember().block().isBot()){
                        sendChannel.createMessage(String.format("ようこそ！%sさん！",message.getAuthor().get().getUsername())).block();
                    }
                }
                else {
                    if (message.getContent().equalsIgnoreCase("!ping")) {
                        return message.getChannel()
                                .flatMap(channel -> channel.createMessage("pong!"));
                    }
                }
                return Mono.empty();
            }).then();
            // combine them!
            return printOnLogin.and(onMessage).and(onJoin);
        });

        login.block();

    }
}

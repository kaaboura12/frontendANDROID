import {
  BadRequestException,
  Body,
  Controller,
  Delete,
  Get,
  InternalServerErrorException,
  Param,
  Post,
  Query,
  UploadedFile,
  UseGuards,
  UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { memoryStorage } from 'multer';
import { MessageService } from './message.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../auth/decorators/user.decorator';
import { ApiBearerAuth, ApiBody, ApiConsumes, ApiOperation, ApiParam, ApiQuery, ApiResponse, ApiTags } from '@nestjs/swagger';
import { CloudinaryService } from './cloudinary.service';
import { Server } from 'socket.io';  // ← ADD THIS
import { WebSocketServer } from '@nestjs/websockets';  // ← ADD THIS

@ApiTags('Messages')
@ApiBearerAuth('JWT-auth')
@UseGuards(JwtAuthGuard)
@Controller('messages')
export class MessageController {
  // ← ADD THIS
  @WebSocketServer()
  server: Server;

  constructor(
    private readonly messageService: MessageService,
    private readonly cloudinaryService: CloudinaryService,
  ) {}
  
  // ... (keep all other methods the same until sendText) ...

  /**
   * Send a text message
   */
  @Post('room/:roomId/text')
  @ApiOperation({ summary: 'Send a text message in a room' })
  @ApiParam({ name: 'roomId', description: 'Room ID' })
  @ApiBody({
    schema: {
      type: 'object',
      properties: {
        text: { type: 'string', example: 'Hello 👋' },
        senderModel: { type: 'string', enum: ['User', 'Child'], example: 'User' },
        senderId: { type: 'string', example: '665f1c9f6e9a5f0984b2d111' },
      },
      required: ['text', 'senderModel', 'senderId'],
    },
  })
  @ApiResponse({ status: 201, description: 'Message sent successfully' })
  async sendText(
    @Param('roomId') roomId: string,
    @Body() body: { text: string; senderModel: 'User' | 'Child'; senderId: string },
    @CurrentUser() currentUser: any,
  ) {
    this.assertObjectId(roomId, 'roomId');
    if (!body?.text || typeof body.text !== 'string') {
      throw new BadRequestException('text is required');
    }
    if (body.senderModel !== 'User' && body.senderModel !== 'Child') {
      throw new BadRequestException('senderModel must be "User" or "Child"');
    }
    this.assertObjectId(body.senderId, 'senderId');
    
    const msg = await this.messageService.sendText(
      {
        roomId,
        text: body.text,
        senderModel: body.senderModel,
        senderId: body.senderId,
      },
      currentUser,
    );

    // ✅ ADD THIS: Broadcast via Socket.IO (same as WebSocket handler)
    if (this.server) {
      this.server.to(`room:${roomId}`).emit('newMessage', msg);
      console.log(`📨 Broadcasted message via REST API to room:${roomId}`);
    }

    return msg;
  }

  /**
   * Send an audio message
   */
  @Post('room/:roomId/audio')
  @ApiOperation({ summary: 'Send an audio message in a room (multipart/form-data)' })
  @ApiParam({ name: 'roomId', description: 'Room ID' })
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      properties: {
        file: { type: 'string', format: 'binary' },
        senderModel: { type: 'string', enum: ['User', 'Child'], example: 'Child' },
        senderId: { type: 'string', example: '665f1c9f6e9a5f0984b2d222' },
        durationSec: { type: 'number', example: 3.2 },
      },
      required: ['file', 'senderModel', 'senderId'],
    },
  })
  @ApiResponse({ status: 201, description: 'Audio message sent successfully' })
  @UseInterceptors(FileInterceptor('file', {
    storage: memoryStorage(),
    limits: { fileSize: 20 * 1024 * 1024 },
  }))
  async sendAudio(
    @Param('roomId') roomId: string,
    @UploadedFile() file: Express.Multer.File,
    @Body() body: { senderModel: 'User' | 'Child'; senderId: string; durationSec?: number },
    @CurrentUser() currentUser: any,
  ) {
    this.assertObjectId(roomId, 'roomId');
    if (!file) {
      throw new BadRequestException('file is required');
    }
    if (body.senderModel !== 'User' && body.senderModel !== 'Child') {
      throw new BadRequestException('senderModel must be "User" or "Child"');
    }
    this.assertObjectId(body.senderId, 'senderId');

    let uploadUrl: string;
    let cloudinaryPublicId: string | null = null;

    if (this.cloudinaryService.enabled()) {
      try {
        const uploadResult = await this.cloudinaryService.uploadAudio(file, {
          folder: `weldiwin/messages/rooms/${roomId}`,
        });
        uploadUrl = uploadResult.secure_url ?? uploadResult.url;
        cloudinaryPublicId = uploadResult.public_id ?? null;
      } catch (error: any) {
        throw new InternalServerErrorException(
          `Failed to upload audio to Cloudinary: ${error?.message ?? 'Unknown error'}`,
        );
      }
    } else {
      // Fall back to base64 data URL
      const base64Data = file.buffer.toString('base64');
      uploadUrl = `data:${file.mimetype};base64,${base64Data}`;
    }

    const durationValue =
      typeof body.durationSec === 'string'
        ? Number(body.durationSec)
        : typeof body.durationSec === 'number'
          ? body.durationSec
          : null;

    const msg = await this.messageService.sendAudio(
      {
        roomId,
        senderModel: body.senderModel,
        senderId: body.senderId,
        audio: {
          url: uploadUrl,
          durationSec: durationValue && Number.isFinite(durationValue) ? durationValue : null,
          mimeType: file.mimetype,
          sizeBytes: file.size,
          cloudinaryPublicId,
        },
      },
      currentUser,
    );

    // ✅ ADD THIS: Broadcast audio messages too
    if (this.server) {
      this.server.to(`room:${roomId}`).emit('newMessage', msg);
      console.log(`📨 Broadcasted audio message via REST API to room:${roomId}`);
    }

    return msg;
  }

  // ... (keep remaining methods the same) ...
}


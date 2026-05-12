import {
  Controller,
  Post,
  UploadedFile,
  UseInterceptors,
  Body,
  BadRequestException,
  Get,
  Param,
  Res,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import type { Response } from 'express';
import { memoryStorage } from 'multer';
import * as path from 'path';
import * as fs from 'fs';
import { LaboratoriosService } from './laboratorios.service';
import { TemplateType } from './types/validation.types';

// Coloca las plantillas .xlsm en backend/templates/
const TEMPLATES_DIR = path.resolve(__dirname, '..', '..', '..', 'templates');

@Controller('laboratorios')
export class LaboratoriosController {
  constructor(private readonly laboratoriosService: LaboratoriosService) {}

  @Post('upload')
  @UseInterceptors(
    FileInterceptor('file', {
      storage: memoryStorage(),
      limits: { fileSize: 25 * 1024 * 1024 },
      fileFilter: (_req, file, cb) => {
        const ext = path.extname(file.originalname).toLowerCase();
        if (['.xlsx', '.xlsm', '.csv'].includes(ext)) {
          cb(null, true);
        } else {
          cb(new BadRequestException('Solo se permiten archivos .xlsx, .xlsm o .csv'), false);
        }
      },
    }),
  )
  uploadFile(
    @UploadedFile() file: Express.Multer.File,
    @Body('templateType') templateType: string,
  ) {
    if (!file) throw new BadRequestException('No se recibió ningún archivo');
    if (!['RRA', 'RRA_FAR'].includes(templateType)) {
      throw new BadRequestException('Tipo de plantilla inválido. Use RRA o RRA_FAR');
    }
    return this.laboratoriosService.validateFile(
      file.buffer,
      file.mimetype,
      templateType as TemplateType,
    );
  }

  @Get('template/:type')
  downloadTemplate(@Param('type') type: string, @Res() res: Response) {
    if (!['RRA', 'RRA_FAR'].includes(type)) {
      throw new BadRequestException('Tipo de plantilla inválido');
    }
    const filename = type === 'RRA' ? 'RRA_plantilla.xlsm' : 'RRA_FAR_plantilla.xlsm';
    const filePath = path.join(TEMPLATES_DIR, filename);
    if (!fs.existsSync(filePath)) {
      return res.status(404).json({ message: `Plantilla no encontrada. Copia el archivo como backend/templates/${filename}` });
    }
    res.download(filePath, filename);
  }
}

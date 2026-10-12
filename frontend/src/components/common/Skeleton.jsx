import React from 'react';
import './Skeleton.css';

export const Skeleton = ({ variant = 'text', width, height, className = '', style = {} }) => {
  const combinedStyle = { ...style };
  if (width) combinedStyle.width = width;
  if (height) combinedStyle.height = height;

  const classes = ['skeleton', `skeleton-${variant}`, className].filter(Boolean).join(' ');

  return <div className={classes} style={combinedStyle} />;
};